package tech.kayys.gamelan.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Four-layer memory hierarchy — complete implementation (Section VIII).
 *
 * <h2>Bug fixes vs previous version</h2>
 * <ul>
 *   <li>{@code recordEpisode} hardcoded the model string {@code "llama3"} when
 *       calling {@code KnowledgeGraphExtractor.extractAsync}. This would fail
 *       on any setup without llama3. Fixed: injects {@link GamelanConfig} and
 *       uses {@code config.defaultModel()}.</li>
 *   <li>{@code buildPromptBlock} called {@code relevantFacts} with a string
 *       built from skill names — not the actual task. Callers now pass the
 *       task directly from the orchestrator context.</li>
 * </ul>
 *
 * <h2>Layer summary</h2>
 * <ul>
 *   <li>Layer 1 — Working Memory: {@link AgentMemory} + {@link tech.kayys.gamelan.session.ConversationSession}</li>
 *   <li>Layer 2 — Episodic: JSON log + vector index via {@link EpisodicIndexer}</li>
 *   <li>Layer 3 — Semantic: vector-backed {@link SemanticMemoryStore}</li>
 *   <li>Layer 4 — Procedural: {@link SemanticMemoryStore} with type=PROCEDURE</li>
 * </ul>
 */
@ApplicationScoped
public class MemoryHierarchy {

    private static final Logger log = LoggerFactory.getLogger(MemoryHierarchy.class);
    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private static final int MAX_EPISODES   = 100;
    private static final int MAX_SEMANTIC   = 8;
    private static final int MAX_PROCEDURAL = 4;

    // ── Layer 2: episodic (recency deque + vector index) ───────────────────
    private final Deque<Episode> episodes = new ConcurrentLinkedDeque<>();

    // ── Layers 3+4: vector-backed ──────────────────────────────────────────
    @Inject SemanticMemoryStore      semanticStore;
    @Inject EpisodicIndexer          episodicIndexer;
    @Inject KnowledgeGraphExtractor  knowledgeExtractor;
    @Inject EmbeddingService         embeddingService;
    @Inject GamelanConfig            config;   // for defaultModel()

    private Path memoryDir;
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "memory-writer"));

    /** Override in tests to redirect persistence. */
    protected Path defaultMemoryDir() {
        String project = Path.of(".").toAbsolutePath().normalize().getFileName().toString();
        return Path.of(System.getProperty("user.home"), ".gamelan", "memory", project);
    }

    @PostConstruct
    void init() {
        memoryDir = defaultMemoryDir();
        try { Files.createDirectories(memoryDir); } catch (IOException ignored) {}
        loadEpisodes();
        log.info("[memory] {} episodes | embedding={}",
                episodes.size(), embeddingService.isAvailable() ? "OK" : "unavailable");
    }

    // ── Layer 2: Episodic ──────────────────────────────────────────────────

    /**
     * Records a completed agent run as an episode, then asynchronously:
     * <ol>
     *   <li>Embeds the episode for semantic retrieval ({@link EpisodicIndexer})</li>
     *   <li>Extracts implicit knowledge from the outcome ({@link KnowledgeGraphExtractor})</li>
     * </ol>
     */
    public void recordEpisode(String task, String outcome, List<String> toolsUsed,
                               boolean success, long durationMs) {
        Episode ep = new Episode(UUID.randomUUID().toString(), task,
                outcome != null ? outcome : "", toolsUsed, success, durationMs, Instant.now());

        episodes.addFirst(ep);
        while (episodes.size() > MAX_EPISODES) episodes.pollLast();
        persistEpisodes();

        // Async: embed for semantic retrieval
        episodicIndexer.indexAsync(ep);

        // Async: extract knowledge from meaningful outcomes using configured model
        if (outcome != null && outcome.length() > 20) {
            knowledgeExtractor.extractAsync(ep, config.defaultModel());
        }

        log.debug("[memory/episodic] recorded success={} task={}", success,
                task.length() > 40 ? task.substring(0, 40) + "…" : task);
    }

    public List<Episode> recentEpisodes(int limit) {
        return episodes.stream().limit(limit).toList();
    }

    public List<Episode> failedEpisodes(int limit) {
        return episodes.stream().filter(e -> !e.success()).limit(limit).toList();
    }

    /**
     * Semantically search past episodes similar to the given task.
     * Requires embedding model. Returns empty list when unavailable.
     */
    public List<EpisodicIndexer.EpisodicMatch> findSimilarEpisodes(String task, int topK) {
        return episodicIndexer.findSimilar(task, topK);
    }

    // ── Layer 3: Semantic ──────────────────────────────────────────────────

    /** Explicitly store a fact (from the REMEMBER protocol). */
    public void learnFact(String topic, String fact) {
        semanticStore.store(topic, fact, "FACT");
    }

    /** Retrieve facts semantically relevant to the given task. */
    public List<SemanticMemoryStore.SemanticFact> relevantFacts(String task, int topK) {
        return semanticStore.search(task, topK, currentProject());
    }

    // ── Layer 4: Procedural ────────────────────────────────────────────────

    /** Store a learned multi-step procedure. */
    public void learnProcedure(String trigger, String steps, String sourceEpisode) {
        semanticStore.store(trigger, "PROCEDURE: " + trigger + " → " + steps,
                "PROCEDURE");
        log.debug("[memory/procedural] stored: {}", trigger);
    }

    /** Retrieve procedures relevant to the given task. */
    public List<SemanticMemoryStore.SemanticFact> relevantProcedures(String task) {
        return semanticStore.search(task, MAX_PROCEDURAL, currentProject()).stream()
                .filter(f -> "PROCEDURE".equals(f.type()))
                .toList();
    }

    // ── Prompt injection ───────────────────────────────────────────────────

    /**
     * Builds the memory section of the system prompt for the given task.
     * Retrieves from all four layers and formats as markdown.
     *
     * @param task the current user task (drives semantic retrieval)
     * @return formatted markdown block, empty if nothing relevant
     */
    public String buildPromptBlock(String task) {
        if (task == null || task.isBlank()) return "";
        StringBuilder sb = new StringBuilder();

        // Layer 3: relevant semantic facts
        List<SemanticMemoryStore.SemanticFact> facts =
                relevantFacts(task, MAX_SEMANTIC);
        List<SemanticMemoryStore.SemanticFact> nonProcedural = facts.stream()
                .filter(f -> !"PROCEDURE".equals(f.type())).toList();

        if (!nonProcedural.isEmpty()) {
            sb.append("## Project Knowledge\n")
              .append("_(learned from past sessions)_\n\n");
            nonProcedural.forEach(f ->
                    sb.append("- **[").append(f.type()).append("] ")
                      .append(f.topic()).append("**: ").append(f.fact()).append("\n"));
            sb.append("\n");
        }

        // Layer 4: relevant procedures
        List<SemanticMemoryStore.SemanticFact> procs = relevantProcedures(task);
        if (!procs.isEmpty()) {
            sb.append("## Effective Procedures\n")
              .append("These approaches worked well for similar tasks:\n\n");
            procs.forEach(p -> sb.append("- ").append(p.fact()).append("\n"));
            sb.append("\n");
        }

        // Layer 2: similar past episodes
        List<EpisodicIndexer.EpisodicMatch> similar = findSimilarEpisodes(task, 4);
        similar.stream().filter(EpisodicIndexer.EpisodicMatch::wasSuccess).limit(2).forEach(m -> {
            if (sb.indexOf("## Similar Past Successes") < 0)
                sb.append("## Similar Past Successes\n");
            sb.append("- ").append(m.preview()).append("\n");
        });
        similar.stream().filter(m -> !m.wasSuccess()).limit(2).forEach(m -> {
            if (sb.indexOf("## Similar Past Failures") < 0)
                sb.append("\n## Similar Past Failures (avoid these patterns)\n");
            sb.append("- ").append(m.preview()).append("\n");
        });
        if (similar.stream().anyMatch(m -> true)) sb.append("\n");

        return sb.toString();
    }

    // ── Statistics ─────────────────────────────────────────────────────────

    public int    episodeCount()         { return episodes.size(); }
    public int    semanticCount()        { return semanticStore.size(); }
    public int    proceduralCount()      {
        return (int) semanticStore.all().stream()
                .filter(f -> "PROCEDURE".equals(f.type())).count();
    }
    public int    indexedEpisodeCount()  { return episodicIndexer.indexedCount(); }
    public double successRate()          {
        if (episodes.isEmpty()) return 1.0;
        return (double) episodes.stream().filter(Episode::success).count() / episodes.size();
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persistEpisodes() {
        writer.submit(() -> {
            try {
                MAPPER.writerWithDefaultPrettyPrinter()
                      .writeValue(memoryDir.resolve("episodes.json").toFile(),
                              new ArrayList<>(episodes));
            } catch (IOException e) {
                log.warn("Episode save failed: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void loadEpisodes() {
        Path f = memoryDir.resolve("episodes.json");
        if (!Files.exists(f)) return;
        try {
            List<Episode> loaded = MAPPER.readValue(f.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Episode.class));
            episodes.addAll(loaded);
        } catch (IOException e) {
            log.warn("Cannot load episodes: {}", e.getMessage());
        }
    }

    private String currentProject() {
        return Path.of(".").toAbsolutePath().normalize().getFileName().toString();
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public record Episode(
            String       id, String task, String outcome,
            List<String> toolsUsed, boolean success,
            long         durationMs, Instant recordedAt
    ) {}

    public record Procedure(
            String trigger, String steps,
            String sourceEpisode, Instant learnedAt
    ) {}
}
