package tech.kayys.gamelan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Checkpoint manager — save and restore agent state across crashes and restarts.
 *
 * <h2>Why this matters</h2>
 * Long-running agent tasks (30+ minutes, 20+ tool calls) can be interrupted by
 * network failures, machine sleep, user Ctrl+C, or OOM errors. Without
 * checkpointing all progress is lost. Claude Code, Aider, and Qwen-Agent all
 * support resumable sessions.
 *
 * <h2>Testability fix</h2>
 * The previous version used a {@code static final Path CHECKPOINT_DIR} field
 * initialised at class-load time. This made unit testing impossible — the field
 * could not be overridden, so tests always wrote to the real user's home
 * directory. Fixed: {@link #checkpointDir()} is a regular instance method that
 * can be overridden in tests via subclassing.
 *
 * <h2>What is checkpointed</h2>
 * Full conversation history (role + content), model, strategy, task, timestamp.
 * Written after every agent turn so at most one LLM call's worth of work is lost.
 */
@ApplicationScoped
public class CheckpointManager {

    private static final Logger log = LoggerFactory.getLogger(CheckpointManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Override in tests via subclass. */
    protected Path checkpointDir() {
        return Path.of(System.getProperty("user.home"), ".gamelan", "checkpoints");
    }

    // ── Save ───────────────────────────────────────────────────────────────

    public void save(ConversationSession session, String task,
                     String model, String strategy) {
        try {
            Path dir = checkpointDir();
            Files.createDirectories(dir);
            Checkpoint cp = new Checkpoint(
                    session.id(), task, model, strategy,
                    session.toMessages().stream()
                            .map(m -> new MessageEntry(m.role(), m.content()))
                            .toList(),
                    Instant.now());
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(dir.resolve(session.id() + ".json").toFile(), cp);
            log.debug("[checkpoint] saved {} turns to {}", session.turnCount(), session.id());
        } catch (IOException e) {
            log.warn("[checkpoint] save failed: {}", e.getMessage());
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────

    public Optional<Checkpoint> load(String idOrPrefix) {
        Path dir = checkpointDir();
        if (!Files.isDirectory(dir)) return Optional.empty();

        try {
            // Exact ID match
            Path exact = dir.resolve(idOrPrefix + ".json");
            if (Files.exists(exact)) {
                return Optional.of(MAPPER.readValue(exact.toFile(), Checkpoint.class));
            }

            // Task-keyword match — pick newest
            try (var stream = Files.list(dir)) {
                return stream.filter(p -> p.toString().endsWith(".json"))
                        .map(p -> readQuietly(p))
                        .filter(Objects::nonNull)
                        .filter(cp -> cp.task() != null
                                && cp.task().toLowerCase().contains(idOrPrefix.toLowerCase()))
                        .max(Comparator.comparing(Checkpoint::savedAt));
            }
        } catch (IOException e) {
            log.warn("[checkpoint] load failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── List ───────────────────────────────────────────────────────────────

    public List<Checkpoint> listAll() {
        Path dir = checkpointDir();
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(this::readQuietly)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Checkpoint::savedAt).reversed())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    public boolean delete(String sessionId) {
        try {
            return Files.deleteIfExists(checkpointDir().resolve(sessionId + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    // ── Restore ────────────────────────────────────────────────────────────

    /** Restores checkpoint history into a fresh ConversationSession. */
    public ConversationSession restore(Checkpoint cp) {
        ConversationSession session = new ConversationSession(cp.sessionId());
        var msgs = cp.messages();
        for (int i = 0; i + 1 < msgs.size(); i += 2) {
            if ("user".equals(msgs.get(i).role())
                    && "assistant".equals(msgs.get(i + 1).role())) {
                session.addTurn(msgs.get(i).content(),
                        AgentResponse.builder().text(msgs.get(i + 1).content()).build());
            }
        }
        log.info("[checkpoint] restored '{}' ({} turns)", cp.sessionId(), session.turnCount());
        return session;
    }

    // ── Private ────────────────────────────────────────────────────────────

    private Checkpoint readQuietly(Path p) {
        try { return MAPPER.readValue(p.toFile(), Checkpoint.class); }
        catch (IOException e) { return null; }
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public record Checkpoint(
            String            sessionId,
            String            task,
            String            model,
            String            strategy,
            List<MessageEntry> messages,
            Instant           savedAt
    ) {
        public String shortId() {
            return sessionId != null && sessionId.length() > 8
                    ? sessionId.substring(0, 8) : sessionId;
        }

        public String taskPreview() {
            if (task == null || task.isBlank()) return "(no task)";
            return task.length() > 60 ? task.substring(0, 60) + "…" : task;
        }
    }

    public record MessageEntry(String role, String content) {}
}
