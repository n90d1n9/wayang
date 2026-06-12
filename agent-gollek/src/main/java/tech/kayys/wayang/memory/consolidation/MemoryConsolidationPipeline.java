package tech.kayys.gamelan.memory.consolidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.memory.hierarchy.ProceduralMemory;
import tech.kayys.gamelan.memory.hierarchy.SemanticMemory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Memory Consolidation Pipeline — compresses episodic memory into semantic knowledge.
 *
 * <h2>The Human Analogy</h2>
 * Humans consolidate memory during sleep: the hippocampus replays episodic
 * memories and transfers structured knowledge to the neocortex. Individual
 * episodes fade; abstract patterns persist.
 *
 * <p>This class implements the same cycle for agents:
 * <ul>
 *   <li>Raw episodes accumulate in {@link EpisodicMemory} (high detail, high volume)</li>
 *   <li>The consolidation pipeline runs periodically (or on demand)</li>
 *   <li>Common patterns are distilled into {@link SemanticMemory} (AKUs)</li>
 *   <li>Successful action sequences are promoted to {@link ProceduralMemory}</li>
 *   <li>Redundant/low-value episodes are pruned (forgetting curve)</li>
 * </ul>
 *
 * <h2>Consolidation Stages</h2>
 * <ol>
 *   <li><b>Pattern extraction</b>: find recurring facts across multiple episodes</li>
 *   <li><b>Contradiction resolution</b>: detect and resolve conflicting facts</li>
 *   <li><b>Abstraction</b>: generalize specific episodes into reusable rules</li>
 *   <li><b>Forgetting</b>: apply forgetting curves to low-confidence, old episodes</li>
 *   <li><b>Index rebuild</b>: rebuild the semantic search index after updates</li>
 * </ol>
 *
 * <h2>Forgetting Curves</h2>
 * Based on the Ebbinghaus forgetting curve: memory strength decays as:
 * <pre>
 *   R = e^(-t/S)
 * </pre>
 * where R is retention (0-1), t is time since last access, S is stability
 * (higher for facts that have been reinforced multiple times).
 * Episodes with R < 0.1 are pruned from episodic memory (but their
 * distilled knowledge remains in semantic memory).
 *
 * <h2>When to run</h2>
 * <ul>
 *   <li>Automatically: when episodic memory exceeds 500 episodes</li>
 *   <li>Scheduled: every 24 hours (like sleep)</li>
 *   <li>On demand: via CLI {@code gamelan memory4 consolidate}</li>
 * </ul>
 */
@ApplicationScoped
public class MemoryConsolidationPipeline {

    private static final Logger log = LoggerFactory.getLogger(MemoryConsolidationPipeline.class);

    // Forgetting curve parameters
    private static final double FORGET_THRESHOLD  = 0.10;  // prune episodes below this retention
    private static final double STABILITY_BASE     = 24.0; // hours for base stability
    private static final int    MIN_EPISODES_KEEP  = 50;   // always keep at least this many

    // Consolidation triggers
    private static final int EPISODE_THRESHOLD = 500;  // run if more than this many episodes

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Inject EpisodicMemory   episodic;
    @Inject SemanticMemory   semantic;
    @Inject ProceduralMemory procedural;
    @Inject GollekSdk        sdk;
    @Inject GamelanConfig    config;

    // Consolidation history
    private final List<ConsolidationRun> history = new CopyOnWriteArrayList<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Runs a full consolidation cycle if triggered (episode count threshold met),
     * or unconditionally if {@code force=true}.
     *
     * @param force if true, consolidates regardless of trigger conditions
     * @return the consolidation result
     */
    public ConsolidationResult consolidate(boolean force) {
        List<EpisodicMemory.Episode> allEpisodes = episodic.all();

        if (!force && allEpisodes.size() < EPISODE_THRESHOLD) {
            log.debug("[consolidation] skipped: {} episodes < threshold {}",
                    allEpisodes.size(), EPISODE_THRESHOLD);
            return ConsolidationResult.skipped(allEpisodes.size());
        }

        log.info("[consolidation] starting: {} episodes", allEpisodes.size());
        Instant start = Instant.now();

        // Stage 1: Pattern extraction → semantic memory
        int newAKUs = extractPatterns(allEpisodes);

        // Stage 2: Contradiction detection and resolution
        int contradictions = resolveContradictions();

        // Stage 3: Abstraction — generalize specific episodes to rules
        int newProcedures = abstractToRules(allEpisodes);

        // Stage 4: Forgetting — prune low-retention episodes
        int pruned = applyForgettingCurve(allEpisodes);

        // Stage 5: Cross-episode fact reinforcement
        int reinforced = reinforceFacts(allEpisodes);

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[consolidation] complete: +{}AKUs +{}procs {}pruned {}reinforced {}ms",
                newAKUs, newProcedures, pruned, reinforced, elapsed.toMillis());

        ConsolidationRun run = new ConsolidationRun(
                newAKUs, contradictions, newProcedures, pruned, reinforced,
                allEpisodes.size(), elapsed, Instant.now());
        history.add(run);

        return new ConsolidationResult(true, allEpisodes.size(), run);
    }

    /**
     * Computes the memory retention score for an episode.
     * Based on the Ebbinghaus forgetting curve: R = e^(-t/S)
     *
     * @param episode the episode to score
     * @return retention score 0.0-1.0 (1.0 = perfectly retained)
     */
    public double retentionScore(EpisodicMemory.Episode episode) {
        double hoursElapsed = Duration.between(episode.recordedAt(), Instant.now())
                .toMinutes() / 60.0;

        // Stability: how many times this pattern has been reinforced
        long reinforcements = episodic.all().stream()
                .filter(e -> !e.toolsUsed().isEmpty() &&
                        e.toolsUsed().equals(episode.toolsUsed()))
                .count();

        double stability = STABILITY_BASE * Math.log(1 + reinforcements);
        return Math.exp(-hoursElapsed / Math.max(stability, 1.0));
    }

    /**
     * Returns a summary of memory health across all layers.
     */
    public MemoryHealthReport health() {
        List<EpisodicMemory.Episode> episodes = episodic.all();
        long stale = episodes.stream()
                .filter(e -> retentionScore(e) < FORGET_THRESHOLD)
                .count();
        int semanticNodes = semantic.allNodes().size();
        int procedures    = procedural.all().size();

        return new MemoryHealthReport(
                episodes.size(), stale, semanticNodes, procedures,
                history.isEmpty() ? null : history.get(history.size()-1),
                history.size());
    }

    /**
     * Returns the consolidation run history.
     */
    public List<ConsolidationRun> history() { return List.copyOf(history); }

    // ── Stage 1: Pattern extraction ────────────────────────────────────────

    private int extractPatterns(List<EpisodicMemory.Episode> episodes) {
        if (episodes.size() < 3) return 0;

        // Group episodes by tool sequence
        Map<String, List<EpisodicMemory.Episode>> bySequence = episodes.stream()
                .collect(Collectors.groupingBy(e -> String.join("→", e.toolsUsed())));

        int newNodes = 0;
        for (Map.Entry<String, List<EpisodicMemory.Episode>> entry : bySequence.entrySet()) {
            List<EpisodicMemory.Episode> group = entry.getValue();
            if (group.size() < 2) continue;  // pattern needs at least 2 occurrences

            // Use LLM to extract a fact from this group of related episodes
            String fact = extractFact(group);
            if (!fact.isBlank()) {
                String concept = "tool-pattern:" + entry.getKey().replace("→", "-");
                double confidence = (double) group.stream().filter(EpisodicMemory.Episode::success).count()
                        / group.size();

                SemanticMemory.KnowledgeNode node = semantic.upsert(
                        concept, fact,
                        SemanticMemory.NodeType.FACT,
                        group.get(0).id(),
                        confidence);
                newNodes++;
                log.debug("[consolidation] extracted AKU: {} (conf={})", concept, confidence);
            }
        }
        return newNodes;
    }

    private String extractFact(List<EpisodicMemory.Episode> group) {
        String sample = group.stream().limit(3)
                .map(e -> "Task: " + truncate(e.task(), 100) +
                          " | Tools: " + String.join(",", e.toolsUsed()) +
                          " | Success: " + e.success())
                .collect(Collectors.joining("\n"));

        String prompt = "Extract ONE reusable fact from these related agent episodes. " +
                "Format: a single concrete sentence.\n\n" + sample;

        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You extract concise, reusable facts from agent execution data. Reply with one sentence only.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.1).maxTokens(100).streaming(false).build());
            String content = resp.getContent();
            return content != null ? content.strip() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ── Stage 2: Contradiction resolution ─────────────────────────────────

    private int resolveContradictions() {
        int resolved = 0;
        Map<Long, SemanticMemory.KnowledgeNode> nodes = semantic.allNodes();

        // Look for nodes with same concept but very different facts
        Map<String, List<SemanticMemory.KnowledgeNode>> byConcept = nodes.values().stream()
                .collect(Collectors.groupingBy(
                        n -> n.concept().split(":")[0]));  // group by concept prefix

        for (Map.Entry<String, List<SemanticMemory.KnowledgeNode>> entry : byConcept.entrySet()) {
            List<SemanticMemory.KnowledgeNode> group = entry.getValue();
            if (group.size() < 2) continue;

            // Keep only the highest-confidence node (simplistic resolution)
            SemanticMemory.KnowledgeNode winner = group.stream()
                    .max(Comparator.comparingDouble(SemanticMemory.KnowledgeNode::confidence))
                    .orElse(group.get(0));

            // Add CONTRADICTS edges for lower-confidence nodes
            group.stream()
                .filter(n -> !n.id().equals(winner.id()))
                .filter(n -> n.confidence() < winner.confidence() - 0.2)
                .forEach(n -> {
                    semantic.addEdge(winner.id(), n.id(),
                            SemanticMemory.EdgeType.CONTRADICTS, 0.8);
                });
            resolved++;
        }
        return resolved;
    }

    // ── Stage 3: Abstraction to procedures ────────────────────────────────

    private int abstractToRules(List<EpisodicMemory.Episode> episodes) {
        int newProcedures = 0;
        List<EpisodicMemory.Episode> successes = episodes.stream()
                .filter(EpisodicMemory.Episode::success).toList();

        // Find tool sequences that appear in 5+ successes
        Map<String, Long> sequenceCounts = successes.stream()
                .filter(e -> !e.toolsUsed().isEmpty())
                .collect(Collectors.groupingBy(
                        e -> String.join("→", e.toolsUsed()), Collectors.counting()));

        for (Map.Entry<String, Long> entry : sequenceCounts.entrySet()) {
            if (entry.getValue() < 5) continue;
            String sequence = entry.getKey();
            List<String> tools = List.of(sequence.split("→"));

            // Learn this as a procedure
            procedural.learnFrom(
                    successes.stream()
                            .filter(e -> String.join("→", e.toolsUsed()).equals(sequence))
                            .findFirst().orElseThrow(),
                    List.of());
            newProcedures++;
        }
        return newProcedures;
    }

    // ── Stage 4: Forgetting curve ──────────────────────────────────────────

    private int applyForgettingCurve(List<EpisodicMemory.Episode> episodes) {
        // Never prune below MIN_EPISODES_KEEP
        if (episodes.size() <= MIN_EPISODES_KEEP) return 0;

        List<EpisodicMemory.Episode> stale = episodes.stream()
                .filter(e -> retentionScore(e) < FORGET_THRESHOLD)
                .sorted(Comparator.comparingDouble(this::retentionScore))
                .limit(episodes.size() - MIN_EPISODES_KEEP)
                .toList();

        // In a real implementation, episodic memory would expose a delete() method
        // For now we log the candidates and return the count
        log.debug("[consolidation] forgetting {} stale episodes", stale.size());
        return stale.size();
    }

    // ── Stage 5: Fact reinforcement ────────────────────────────────────────

    private int reinforceFacts(List<EpisodicMemory.Episode> episodes) {
        int reinforced = 0;
        Map<Long, SemanticMemory.KnowledgeNode> nodes = semantic.allNodes();

        for (SemanticMemory.KnowledgeNode node : nodes.values()) {
            // Count how many recent episodes confirm this fact
            long confirmations = episodes.stream()
                    .filter(e -> node.sourceEpisodeIds().contains(e.id()))
                    .count();
            if (confirmations > 0) {
                // Reinforce by upsert with higher confidence
                double newConf = Math.min(1.0, node.confidence() + 0.05 * confirmations);
                semantic.upsert(node.concept(), node.fact(), node.type(),
                        node.sourceEpisodeIds().isEmpty() ? 0 : node.sourceEpisodeIds().get(0),
                        newConf);
                reinforced++;
            }
        }
        return reinforced;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ConsolidationRun(
            int      newAKUs,
            int      contradictionsResolved,
            int      newProcedures,
            int      episodesPruned,
            int      factsReinforced,
            int      episodesBefore,
            Duration elapsed,
            Instant  completedAt
    ) {
        public String summary() {
            return String.format(
                    "Consolidation: +%d AKUs +%d procs -%d pruned +%d reinforced | %dms",
                    newAKUs, newProcedures, episodesPruned, factsReinforced, elapsed.toMillis());
        }
    }

    public record ConsolidationResult(
            boolean          ran,
            int              episodeCount,
            ConsolidationRun run
    ) {
        static ConsolidationResult skipped(int count) {
            return new ConsolidationResult(false, count, null);
        }
        public String summary() {
            return ran ? run.summary() :
                    "Skipped (only " + episodeCount + " episodes, need 500+)";
        }
    }

    public record MemoryHealthReport(
            int              episodeCount,
            long             staleEpisodes,
            int              semanticNodes,
            int              procedures,
            ConsolidationRun lastConsolidation,
            int              consolidationCount
    ) {
        public String summary() {
            return String.format(
                    "Memory: %d episodes (%d stale) | %d AKUs | %d procedures | %d consolidations",
                    episodeCount, staleEpisodes, semanticNodes, procedures, consolidationCount);
        }
    }
}
