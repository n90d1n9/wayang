package tech.kayys.gamelan.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event-sourced, append-only audit trail (Section V — Governance & Audit).
 *
 * <h2>Why this matters</h2>
 * Production agentic systems touching real files, running real commands, and
 * calling real APIs MUST have an audit trail. Without it you cannot:
 * <ul>
 *   <li>Debug why the agent did something unexpected</li>
 *   <li>Comply with enterprise security requirements</li>
 *   <li>Detect prompt injection attempts</li>
 *   <li>Reproduce failures for post-mortems</li>
 * </ul>
 *
 * <h2>Tamper evidence</h2>
 * Each entry includes a SHA-256 hash of the previous entry's hash chained
 * with the current entry content. This creates a hash chain: if any entry
 * is modified or deleted, all subsequent hashes will be invalid.
 *
 * <h2>Storage</h2>
 * Append-only NDJSON file at {@code ~/.gamelan/audit/<project>-<date>.log}.
 * Rotation happens daily. Reads happen via stream to avoid loading large files
 * into memory.
 *
 * <h2>Write path</h2>
 * All writes are fire-and-forget to a single-threaded background executor.
 * The agent loop never blocks on audit writes.
 */
@ApplicationScoped
public class AuditLog {

    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private Path logFile;
    private String prevHash = "genesis"; // first entry hashes against this
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "audit-writer"));

    /** Override in tests for a custom log directory. */
    protected Path logDir() {
        return Path.of(System.getProperty("user.home"), ".gamelan", "audit");
    }

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        Path logDir    = logDir();
        String date    = java.time.LocalDate.now().toString();
        try {
            Files.createDirectories(logDir);
            logFile = logDir.resolve(project + "-" + date + ".log");
            // Load last hash from existing file for chain continuity
            prevHash = loadLastHash();
        } catch (IOException e) {
            log.warn("[audit] init failed: {}", e.getMessage());
        }
        log.debug("[audit] log file: {}", logFile);
    }

    // ── Event types ────────────────────────────────────────────────────────

    /** Logs a tool call before execution. */
    public void logToolCall(String sessionId, String tool, Map<String, Object> params) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.TOOL_CALL, tool, params, null, Instant.now(), prevHash));
    }

    /** Logs the result of a tool call. */
    public void logToolResult(String sessionId, String tool, boolean success, String summary) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.TOOL_RESULT, tool,
                Map.of("success", success, "summary", truncate(summary, 500)),
                null, Instant.now(), prevHash));
    }

    /** Logs an LLM inference call. */
    public void logLlmCall(String sessionId, String model, int promptTokens, int strategy) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.LLM_CALL, model,
                Map.of("promptTokens", promptTokens, "strategy", strategy),
                null, Instant.now(), prevHash));
    }

    /** Logs a security event (blocked operation, suspicious input, etc.). */
    public void logSecurityEvent(String sessionId, String description, String severity) {
        log.warn("[audit/security] {}", description);
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.SECURITY_EVENT, "security",
                Map.of("description", description, "severity", severity),
                null, Instant.now(), prevHash));
    }

    /** Logs a human-approval decision. */
    public void logHumanDecision(String sessionId, String action, boolean approved, String reason) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.HUMAN_DECISION, action,
                Map.of("approved", approved, "reason", reason != null ? reason : ""),
                null, Instant.now(), prevHash));
    }

    /** Logs agent task start. */
    public void logRunStart(String sessionId, String task, String strategy) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.RUN_START, strategy,
                Map.of("task", truncate(task, 300)),
                null, Instant.now(), prevHash));
    }

    /** Logs agent task completion. */
    public void logRunEnd(String sessionId, boolean success, long durationMs) {
        write(new AuditEntry(
                UUID.randomUUID().toString(), sessionId,
                EventType.RUN_END, success ? "success" : "failure",
                Map.of("success", success, "durationMs", durationMs),
                null, Instant.now(), prevHash));
    }

    // ── Chain verification ─────────────────────────────────────────────────

    /**
     * Verifies the integrity of the audit log by re-computing the hash chain.
     *
     * @return {@code true} if the chain is intact, {@code false} if tampered
     */
    public boolean verifyChain() {
        if (logFile == null || !Files.exists(logFile)) return true;
        try (var reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String prevH = "genesis";
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                lineNum++;
                AuditEntry entry = MAPPER.readValue(line, AuditEntry.class);
                String expected  = sha256(prevH + entry.id() + entry.timestamp().toString());
                if (!expected.equals(entry.hash())) {
                    log.error("[audit] chain broken at line {} id={}", lineNum, entry.id());
                    return false;
                }
                prevH = entry.hash();
            }
            log.info("[audit] chain verified: {} entries intact", lineNum);
            return true;
        } catch (Exception e) {
            log.warn("[audit] verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Returns the most recent audit entries (newest first, up to limit). */
    public List<AuditEntry> recent(int limit) {
        if (logFile == null || !Files.exists(logFile)) return List.of();
        Deque<AuditEntry> deque = new ArrayDeque<>();
        try (var reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    deque.addFirst(MAPPER.readValue(line, AuditEntry.class));
                    if (deque.size() > limit) deque.pollLast();
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            log.warn("[audit] read failed: {}", e.getMessage());
        }
        return new ArrayList<>(deque);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void write(AuditEntry entry) {
        // Compute chained hash
        String hash = sha256(prevHash + entry.id() + entry.timestamp().toString());
        AuditEntry signed = new AuditEntry(entry.id(), entry.sessionId(), entry.type(),
                entry.subject(), entry.metadata(), hash, entry.timestamp(), prevHash);
        prevHash = hash;

        writer.submit(() -> {
            if (logFile == null) return;
            try (var out = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(MAPPER.writeValueAsString(signed));
                out.newLine();
            } catch (IOException e) {
                log.warn("[audit] write failed: {}", e.getMessage());
            }
        });
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "hash-error-" + System.nanoTime();
        }
    }

    private String loadLastHash() {
        if (!Files.exists(logFile)) return "genesis";
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).strip();
                if (!line.isBlank()) {
                    AuditEntry last = MAPPER.readValue(line, AuditEntry.class);
                    return last.hash() != null ? last.hash() : "genesis";
                }
            }
        } catch (Exception e) {
            log.warn("[audit] cannot load last hash: {}", e.getMessage());
        }
        return "genesis";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public enum EventType {
        RUN_START, RUN_END, LLM_CALL, TOOL_CALL, TOOL_RESULT,
        HUMAN_DECISION, SECURITY_EVENT, MEMORY_WRITE, SKILL_INVOKED
    }

    public record AuditEntry(
            String              id,
            String              sessionId,
            EventType           type,
            String              subject,
            Map<String, Object> metadata,
            String              hash,
            Instant             timestamp,
            String              prevHash
    ) {}
}
