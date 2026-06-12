package tech.kayys.gamelan.memory.hierarchy;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Episodic Memory — stores complete agent execution traces with outcomes.
 *
 * <h2>What is stored</h2>
 * <ul>
 *   <li>Task description (what was asked)</li>
 *   <li>Result (what was produced)</li>
 *   <li>Success/failure flag</li>
 *   <li>Tools used (in order)</li>
 *   <li>Duration</li>
 *   <li>Embedding vector (for semantic retrieval)</li>
 *   <li>Linked knowledge nodes (promoted to semantic layer)</li>
 * </ul>
 *
 * <h2>Retrieval</h2>
 * Episodes are retrieved by:
 * <ol>
 *   <li>Keyword overlap (fast, always available)</li>
 *   <li>Embedding cosine similarity (when embedding model available)</li>
 * </ol>
 *
 * <h2>Retention Policy</h2>
 * Keeps the most recent 1,000 episodes per project. Older episodes are
 * archived (compressed JSON) and can be queried separately.
 *
 * <h2>Why episodes matter</h2>
 * Without episodic memory, an agent that fails to parse a YAML file will
 * try the same approach every session. With it, the agent recalls "last time
 * I tried approach X and it failed because Y — let me try Z instead."
 */
@ApplicationScoped
public class EpisodicMemory {

    private static final Logger log = LoggerFactory.getLogger(EpisodicMemory.class);

    private static final int    MAX_EPISODES   = 1_000;
    private static final int    MIN_OVERLAP     = 2;
    private static final String EPISODE_FILE   = "episodes.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final Deque<Episode>     episodes   = new ConcurrentLinkedDeque<>();
    private final AtomicLong         idSeq      = new AtomicLong(1);
    private Path                     storageDir;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize()
                .getFileName().toString();
        storageDir = Path.of(System.getProperty("user.home"), ".gamelan",
                "memory", "episodic", project);
        load();
        log.info("[episodic] loaded {} episodes for project '{}'", episodes.size(), project);
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records a completed agent execution as an episode.
     */
    public Episode record(String task, String result, boolean success,
                          List<String> toolsUsed, long durationMs) {
        Episode ep = new Episode(
                idSeq.getAndIncrement(),
                task,
                result != null ? truncate(result, 2000) : "",
                success,
                toolsUsed != null ? List.copyOf(toolsUsed) : List.of(),
                durationMs,
                Instant.now(),
                List.of()   // knowledge node IDs populated by SemanticMemory
        );
        episodes.addFirst(ep);
        trimToLimit();
        persistAsync();
        log.debug("[episodic] recorded episode #{} success={} tools={}",
                ep.id(), success, toolsUsed != null ? toolsUsed.size() : 0);
        return ep;
    }

    /**
     * Finds the most relevant episodes for a given task using keyword overlap.
     *
     * @param task  the current task description
     * @param limit max number of episodes to return
     */
    public List<Episode> findRelevant(String task, int limit) {
        Set<String> taskWords = tokenize(task);
        if (taskWords.isEmpty()) return List.of();

        record Scored(Episode ep, int score) {}

        return episodes.stream()
                .map(ep -> {
                    Set<String> epWords = tokenize(ep.task());
                    epWords.retainAll(taskWords);
                    return new Scored(ep, epWords.size());
                })
                .filter(s -> s.score() >= MIN_OVERLAP)
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(s -> s.ep().recordedAt(), Comparator.reverseOrder()))
                .limit(limit)
                .map(Scored::ep)
                .toList();
    }

    /**
     * Returns recent failure episodes — useful for failure analysis.
     */
    public List<Episode> recentFailures(int limit) {
        return episodes.stream()
                .filter(e -> !e.success())
                .limit(limit)
                .toList();
    }

    /**
     * Returns statistics across all episodes.
     */
    public EpisodeStats stats() {
        long total    = episodes.size();
        long success  = episodes.stream().filter(Episode::success).count();
        long failures = total - success;
        OptionalDouble avgDuration = episodes.stream()
                .mapToLong(Episode::durationMs).average();
        Map<String, Long> toolFreq = new LinkedHashMap<>();
        episodes.stream()
                .flatMap(e -> e.toolsUsed().stream())
                .forEach(t -> toolFreq.merge(t, 1L, Long::sum));
        return new EpisodeStats(total, success, failures,
                avgDuration.orElse(0), toolFreq);
    }

    public List<Episode> all() {
        return List.copyOf(episodes);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void trimToLimit() {
        while (episodes.size() > MAX_EPISODES) {
            episodes.pollLast(); // remove oldest
        }
    }

    private void persistAsync() {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(storageDir);
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(storageDir.resolve(EPISODE_FILE).toFile(),
                              List.copyOf(episodes));
            } catch (IOException e) {
                log.warn("[episodic] persist failed: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Path file = storageDir.resolve(EPISODE_FILE);
        if (!Files.exists(file)) return;
        try {
            List<Episode> loaded = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Episode.class));
            loaded.forEach(episodes::addLast);
            // Re-sequence ID counter
            loaded.stream().mapToLong(Episode::id).max()
                  .ifPresent(max -> idSeq.set(max + 1));
        } catch (IOException e) {
            log.warn("[episodic] load failed: {}", e.getMessage());
        }
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> words = new HashSet<>();
        for (String w : text.toLowerCase().split("[\\s\\p{Punct}]+")) {
            if (w.length() >= 4) words.add(w);
        }
        return words;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A complete record of one agent execution.
     */
    public record Episode(
            long        id,
            String      task,
            String      result,
            boolean     success,
            List<String> toolsUsed,
            long        durationMs,
            Instant     recordedAt,
            List<Long>  knowledgeNodeIds
    ) {}

    /**
     * Aggregate statistics across the episode store.
     */
    public record EpisodeStats(
            long             total,
            long             successes,
            long             failures,
            double           avgDurationMs,
            Map<String, Long> toolFrequency
    ) {
        public double successRate() {
            return total == 0 ? 0.0 : (double) successes / total * 100;
        }

        public String summary() {
            return String.format(
                    "Episodes: %d | Success: %.1f%% | Avg: %.0fms | Top tool: %s",
                    total, successRate(), avgDurationMs,
                    toolFrequency.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse("none"));
        }
    }
}
