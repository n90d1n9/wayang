package tech.kayys.gamelan.memory.hierarchy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Unified facade over the 4-layer memory hierarchy.
 *
 * <h2>Layers</h2>
 * <ol>
 *   <li><b>Working Memory</b>  — session-scoped context (existing ConversationSession)</li>
 *   <li><b>Episodic Memory</b> — past runs with decisions, failures, outcomes</li>
 *   <li><b>Semantic Memory</b> — extracted knowledge graph from episodes (AKUs)</li>
 *   <li><b>Procedural Memory</b> — learned "how-to" strategies from repeated success</li>
 * </ol>
 *
 * <h2>Memory Promotion Pipeline</h2>
 * <pre>
 * Episode (raw)
 *   → Semantic Extraction  (key facts, entities, relations)
 *   → Procedural Learning  (if success-rate > threshold on pattern)
 * </pre>
 *
 * This is the critical missing layer that enables true cross-run accumulation
 * and meta-learning, transforming the agent from a stateless tool into a
 * continuously improving system.
 */
@ApplicationScoped
public class MemoryHierarchy {

    private static final Logger log = LoggerFactory.getLogger(MemoryHierarchy.class);

    @Inject EpisodicMemory    episodic;
    @Inject SemanticMemory    semantic;
    @Inject ProceduralMemory  procedural;

    // ── Unified retrieval ──────────────────────────────────────────────────

    /**
     * Retrieves the most relevant context across all memory layers for
     * a given task. Returns a structured prompt block ready for injection.
     */
    public MemoryContext retrieve(String task) {
        log.debug("[memory-hierarchy] retrieving context for task: {}",
                task.length() > 80 ? task.substring(0, 80) + "…" : task);

        List<EpisodicMemory.Episode>         episodes    = episodic.findRelevant(task, 3);
        List<SemanticMemory.KnowledgeNode>   knowledge   = semantic.query(task, 5);
        List<ProceduralMemory.Procedure>     procedures  = procedural.findApplicable(task, 2);

        return new MemoryContext(episodes, knowledge, procedures);
    }

    /**
     * Records the completion of an agent task, triggering the memory
     * promotion pipeline asynchronously.
     */
    public void record(String task, String result, boolean success,
                       List<String> toolsUsed, long durationMs) {
        EpisodicMemory.Episode episode =
                episodic.record(task, result, success, toolsUsed, durationMs);

        // Promote facts from episode → semantic memory
        List<SemanticMemory.KnowledgeNode> extracted = semantic.extractFrom(episode);
        log.debug("[memory-hierarchy] extracted {} knowledge nodes from episode {}",
                extracted.size(), episode.id());

        // Promote successful patterns → procedural memory
        if (success) {
            procedural.learnFrom(episode, extracted);
        }
    }

    /**
     * Builds a system-prompt injection block from all relevant memory layers.
     */
    public String buildPromptBlock(String task) {
        MemoryContext ctx = retrieve(task);
        if (ctx.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Cross-Session Memory\n\n");

        if (!ctx.procedures().isEmpty()) {
            sb.append("### How to Solve This (Learned Strategies)\n");
            ctx.procedures().forEach(p ->
                sb.append("- **").append(p.name()).append("**: ").append(p.description()).append("\n"));
            sb.append("\n");
        }

        if (!ctx.knowledge().isEmpty()) {
            sb.append("### Relevant Knowledge\n");
            ctx.knowledge().forEach(k ->
                sb.append("- **").append(k.concept()).append("**: ").append(k.fact()).append("\n"));
            sb.append("\n");
        }

        if (!ctx.episodes().isEmpty()) {
            sb.append("### Relevant Past Experiences\n");
            ctx.episodes().forEach(e -> {
                String outcome = e.success() ? "✓ Succeeded" : "✗ Failed";
                sb.append("- ").append(outcome).append(" [").append(e.durationMs()).append("ms]: ")
                  .append(e.task().length() > 100 ? e.task().substring(0, 100) + "…" : e.task())
                  .append("\n");
                if (!e.success() && !e.result().isBlank()) {
                    sb.append("  Failure reason: ").append(
                            e.result().length() > 200 ? e.result().substring(0, 200) + "…" : e.result())
                      .append("\n");
                }
            });
        }

        return sb.toString();
    }

    /** Structured result from multi-layer retrieval. */
    public record MemoryContext(
            List<EpisodicMemory.Episode>       episodes,
            List<SemanticMemory.KnowledgeNode> knowledge,
            List<ProceduralMemory.Procedure>   procedures
    ) {
        public boolean isEmpty() {
            return episodes.isEmpty() && knowledge.isEmpty() && procedures.isEmpty();
        }
    }
}
