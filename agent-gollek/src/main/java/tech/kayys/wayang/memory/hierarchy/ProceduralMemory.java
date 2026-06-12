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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Procedural Memory — learned "how-to" strategies extracted from repeated success.
 *
 * <h2>What is a Procedure</h2>
 * A procedure is a named, reusable strategy discovered by the agent through
 * pattern recognition across successful episodes:
 * <pre>
 * name:        "fix-null-pointer"
 * description: "When encountering NPE: read stack trace → identify null → add Optional/null check"
 * steps:       ["read_file with stack trace line", "search_files for null assignment", "apply_patch"]
 * triggers:    ["NPE", "NullPointerException", "null pointer"]
 * successRate: 0.87
 * usageCount:  23
 * </pre>
 *
 * <h2>Learning Algorithm</h2>
 * <ol>
 *   <li>Episode completes with success=true</li>
 *   <li>Extract tool call sequence → "action pattern"</li>
 *   <li>Find existing procedures with similar triggers (token overlap)</li>
 *   <li>If match found: increment usage, update success rate</li>
 *   <li>If no match AND pattern appears ≥3 times: promote to named procedure</li>
 * </ol>
 *
 * <h2>Why this matters</h2>
 * This is what gives the agent "muscle memory" — after solving 10 similar
 * debugging problems, it stops treating each as novel and applies the proven
 * procedure immediately, cutting tool calls by 60-80%.
 */
@ApplicationScoped
public class ProceduralMemory {

    private static final Logger log = LoggerFactory.getLogger(ProceduralMemory.class);

    private static final int    PROMOTION_THRESHOLD = 3;  // min pattern occurrences before promotion
    private static final double MIN_SUCCESS_RATE    = 0.6;
    private static final int    MAX_PROCEDURES      = 200;
    private static final String PROCS_FILE          = "procedures.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final Map<Long, Procedure>   procedures  = new ConcurrentHashMap<>();
    private final Map<String, Integer>   patternHits = new ConcurrentHashMap<>(); // pattern → count
    private final AtomicLong             idSeq       = new AtomicLong(1);
    private Path                         storageDir;

    @PostConstruct
    void init() {
        String project = Path.of(".").toAbsolutePath().normalize()
                .getFileName().toString();
        storageDir = Path.of(System.getProperty("user.home"), ".gamelan",
                "memory", "procedural", project);
        load();
        log.info("[procedural] {} procedures loaded", procedures.size());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Learns from a successful episode.
     * Extracts action patterns and promotes to procedures when threshold is met.
     */
    public void learnFrom(EpisodicMemory.Episode episode,
                          List<SemanticMemory.KnowledgeNode> knowledgeNodes) {
        if (!episode.success() || episode.toolsUsed().isEmpty()) return;

        String pattern = buildPattern(episode);
        int hits = patternHits.merge(pattern, 1, Integer::sum);

        log.debug("[procedural] pattern '{}' hits={}", truncate(pattern, 60), hits);

        if (hits >= PROMOTION_THRESHOLD) {
            promoteOrReinforce(episode, pattern, knowledgeNodes);
        }
    }

    /**
     * Finds applicable procedures for a given task.
     * Returns procedures sorted by relevance × success rate.
     */
    public List<Procedure> findApplicable(String task, int limit) {
        Set<String> taskWords = tokenize(task);
        if (taskWords.isEmpty()) return List.of();

        record Scored(Procedure p, double score) {}

        return procedures.values().stream()
                .filter(p -> p.successRate() >= MIN_SUCCESS_RATE)
                .map(p -> {
                    Set<String> triggerWords = new HashSet<>();
                    p.triggers().forEach(t -> triggerWords.addAll(tokenize(t)));
                    triggerWords.retainAll(taskWords);
                    double relevance = (double) triggerWords.size() /
                            Math.max(1, p.triggers().size());
                    return new Scored(p, relevance * p.successRate() * Math.log(p.usageCount() + 1));
                })
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(limit)
                .map(Scored::p)
                .toList();
    }

    /**
     * Manually registers a procedure (e.g., from user definition or skill).
     */
    public Procedure register(String name, String description,
                               List<String> steps, List<String> triggers) {
        Procedure p = new Procedure(
                idSeq.getAndIncrement(), name, description,
                List.copyOf(steps), List.copyOf(triggers),
                1.0, 0, Instant.now());
        procedures.put(p.id(), p);
        persist();
        return p;
    }

    /**
     * Records success or failure of a procedure execution.
     * Updates the rolling success rate using exponential moving average.
     */
    public void recordOutcome(long procedureId, boolean success) {
        Procedure p = procedures.get(procedureId);
        if (p == null) return;

        double alpha = 0.1; // EMA factor
        double newRate = alpha * (success ? 1.0 : 0.0) + (1 - alpha) * p.successRate();
        Procedure updated = new Procedure(
                p.id(), p.name(), p.description(), p.steps(), p.triggers(),
                newRate, p.usageCount() + 1, Instant.now());
        procedures.put(updated.id(), updated);
        persist();
    }

    public List<Procedure> all() {
        return procedures.values().stream()
                .sorted(Comparator.comparingDouble(Procedure::successRate).reversed())
                .toList();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void promoteOrReinforce(EpisodicMemory.Episode episode, String pattern,
                                    List<SemanticMemory.KnowledgeNode> knowledge) {
        // Check if procedure already exists for this pattern
        Optional<Procedure> existing = procedures.values().stream()
                .filter(p -> p.steps().equals(episode.toolsUsed()))
                .findFirst();

        if (existing.isPresent()) {
            recordOutcome(existing.get().id(), true);
        } else {
            // Promote to new procedure
            List<String> triggers = extractTriggers(episode.task(), knowledge);
            String name = "auto-" + sanitize(episode.toolsUsed().get(0))
                    + "-" + (procedures.size() + 1);
            String description = buildDescription(episode);
            Procedure p = new Procedure(
                    idSeq.getAndIncrement(), name, description,
                    List.copyOf(episode.toolsUsed()),
                    List.copyOf(triggers), 1.0, 1, Instant.now());
            procedures.put(p.id(), p);
            persist();
            log.info("[procedural] promoted new procedure '{}' from {} episodes",
                    name, PROMOTION_THRESHOLD);
        }
    }

    private String buildPattern(EpisodicMemory.Episode ep) {
        return String.join("→", ep.toolsUsed());
    }

    private List<String> extractTriggers(String task,
                                         List<SemanticMemory.KnowledgeNode> knowledge) {
        List<String> triggers = new ArrayList<>(tokenize(task).stream()
                .filter(w -> w.length() >= 5).toList());
        knowledge.stream()
                .map(SemanticMemory.KnowledgeNode::concept)
                .forEach(triggers::add);
        return triggers.stream().distinct().limit(10).toList();
    }

    private String buildDescription(EpisodicMemory.Episode ep) {
        return String.format("Learned strategy: %s → %s (success=true, duration=%dms)",
                truncate(ep.task(), 100),
                String.join(" → ", ep.toolsUsed()),
                ep.durationMs());
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        Set<String> words = new HashSet<>();
        for (String w : text.toLowerCase().split("[\\s\\p{Punct}]+")) {
            if (w.length() >= 4) words.add(w);
        }
        return words;
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-z0-9]", "-").toLowerCase();
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void persist() {
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(storageDir);
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(storageDir.resolve(PROCS_FILE).toFile(),
                              new ArrayList<>(procedures.values()));
            } catch (IOException e) {
                log.warn("[procedural] persist failed: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Path file = storageDir.resolve(PROCS_FILE);
        if (!Files.exists(file)) return;
        try {
            List<Procedure> loaded = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Procedure.class));
            loaded.forEach(p -> procedures.put(p.id(), p));
            loaded.stream().mapToLong(Procedure::id).max()
                  .ifPresent(max -> idSeq.set(max + 1));
        } catch (IOException e) {
            log.warn("[procedural] load failed: {}", e.getMessage());
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A learned, reusable problem-solving strategy.
     */
    public record Procedure(
            long         id,
            String       name,
            String       description,
            List<String> steps,       // ordered tool call sequence
            List<String> triggers,    // keywords that suggest this procedure applies
            double       successRate, // 0.0–1.0, EMA-updated
            int          usageCount,
            Instant      updatedAt
    ) {
        public String promptSnippet() {
            return String.format("**%s** (%.0f%% success rate): %s → [%s]",
                    name, successRate * 100, description,
                    String.join(", ", steps));
        }
    }
}
