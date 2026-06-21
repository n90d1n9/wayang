package tech.kayys.gamelan.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-session persistent memory for the agent.
 *
 * <h2>Why this matters</h2>
 * Claude Code, Cursor, and Qwen-Agent all remember things across sessions:
 * user preferences, project-specific conventions, frequently used commands,
 * past decisions. Without this, every session starts from zero — the agent
 * asks the same clarifying questions repeatedly and forgets past context.
 *
 * <h2>Memory types</h2>
 * <ul>
 *   <li><b>FACT</b> — declarative facts ("this project uses Lombok", "Java 21")</li>
 *   <li><b>PREFERENCE</b> — user coding preferences ("prefer records over POJOs")</li>
 *   <li><b>DECISION</b> — architectural decisions with reasoning</li>
 *   <li><b>COMMAND</b> — frequently used commands for this project</li>
 * </ul>
 *
 * <h2>Storage</h2>
 * JSON file at {@code ~/.gamelan/memory/<project-name>.json}.
 * Each memory entry has a timestamp and optional TTL.
 *
 * <h2>Injection</h2>
 * Called by {@link tech.kayys.gamelan.agent.PromptBuilder} to prepend
 * relevant memories to each system prompt. Only memories matching the
 * current project or marked global are included.
 */
@ApplicationScoped
public class AgentMemory {

    private static final Logger log = LoggerFactory.getLogger(AgentMemory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final int MAX_MEMORIES_IN_PROMPT = 20;
    private static final Path MEMORY_DIR =
            Path.of(System.getProperty("user.home"), ".gamelan", "memory");

    private final Map<String, MemoryEntry> entries = new ConcurrentHashMap<>();
    private String projectKey;

    @PostConstruct
    void init() {
        projectKey = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        load();
        log.info("Agent memory: {} entries for project '{}'", entries.size(), projectKey);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Store a memory entry. Overwrites if key already exists. */
    public void remember(String key, String value, MemoryType type) {
        entries.put(key, new MemoryEntry(key, value, type, projectKey, Instant.now()));
        persist();
        log.debug("Remembered [{}]: {}", type, key);
    }

    /** Store a global memory (applies across all projects). */
    public void rememberGlobal(String key, String value, MemoryType type) {
        entries.put(key, new MemoryEntry(key, value, type, "_global", Instant.now()));
        persist();
    }

    /** Delete a memory entry. */
    public void forget(String key) {
        entries.remove(key);
        persist();
    }

    /** Returns all memory entries relevant to the current project. */
    public List<MemoryEntry> relevant() {
        return entries.values().stream()
                .filter(e -> "_global".equals(e.project()) || projectKey.equals(e.project()))
                .sorted(Comparator.comparing(MemoryEntry::savedAt).reversed())
                .limit(MAX_MEMORIES_IN_PROMPT)
                .toList();
    }

    /** Returns all entries. */
    public List<MemoryEntry> all() {
        return entries.values().stream()
                .sorted(Comparator.comparing(MemoryEntry::savedAt).reversed())
                .toList();
    }

    /**
     * Builds the memory block for injection into the system prompt.
     * Returns empty string if no relevant memories exist.
     */
    public String promptBlock() {
        List<MemoryEntry> rel = relevant();
        if (rel.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Remembered Context\n");
        sb.append("(From previous sessions — trust these as background knowledge.)\n\n");

        Map<MemoryType, List<MemoryEntry>> byType = new LinkedHashMap<>();
        for (MemoryType t : MemoryType.values()) byType.put(t, new ArrayList<>());
        for (MemoryEntry e : rel) byType.get(e.type()).add(e);

        for (MemoryType type : MemoryType.values()) {
            List<MemoryEntry> group = byType.get(type);
            if (group.isEmpty()) continue;
            sb.append("### ").append(type.label()).append("\n");
            for (MemoryEntry e : group) {
                sb.append("- **").append(e.key()).append("**: ").append(e.value()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Parse memory commands from LLM output (e.g. REMEMBER: key = value). */
    public int extractAndStore(String llmOutput) {
        if (llmOutput == null) return 0;
        int count = 0;
        for (String line : llmOutput.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("REMEMBER:") || trimmed.startsWith("🧠 REMEMBER:")) {
                String payload = trimmed.replaceFirst(".*REMEMBER:", "").strip();
                int eq = payload.indexOf('=');
                if (eq > 0) {
                    String key   = payload.substring(0, eq).strip();
                    String value = payload.substring(eq + 1).strip();
                    MemoryType type = inferType(value);
                    remember(key, value, type);
                    count++;
                }
            }
        }
        return count;
    }

    // ── Private ────────────────────────────────────────────────────────────

    private MemoryType inferType(String value) {
        String lower = value.toLowerCase();
        if (lower.contains("prefer") || lower.contains("always") || lower.contains("never"))
            return MemoryType.PREFERENCE;
        if (lower.contains("decided") || lower.contains("architecture") || lower.contains("design"))
            return MemoryType.DECISION;
        if (lower.startsWith("mvn") || lower.startsWith("npm") || lower.startsWith("./"))
            return MemoryType.COMMAND;
        return MemoryType.FACT;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Path file = memoryFile();
        if (!Files.exists(file)) return;
        try {
            List<MemoryEntry> loaded = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, MemoryEntry.class));
            loaded.forEach(e -> entries.put(e.key(), e));
        } catch (IOException e) {
            log.warn("Cannot load memory from {}: {}", file, e.getMessage());
        }
    }

    private void persist() {
        try {
            Files.createDirectories(MEMORY_DIR);
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(memoryFile().toFile(), new ArrayList<>(entries.values()));
        } catch (IOException e) {
            log.warn("Cannot persist memory: {}", e.getMessage());
        }
    }

    private Path memoryFile() {
        return MEMORY_DIR.resolve(projectKey + ".json");
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public enum MemoryType {
        FACT("Facts & Project Info"),
        PREFERENCE("User Preferences"),
        DECISION("Architecture Decisions"),
        COMMAND("Useful Commands");

        private final String label;
        MemoryType(String label) { this.label = label; }
        public String label() { return label; }
    }

    public record MemoryEntry(
            String key, String value, MemoryType type,
            String project, Instant savedAt
    ) {}
}
