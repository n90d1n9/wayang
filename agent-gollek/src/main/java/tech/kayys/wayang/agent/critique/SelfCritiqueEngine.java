package tech.kayys.gamelan.agent.critique;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.agent.routing.WorkloadModelRouter;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.time.*;
import java.util.*;

/**
 * SelfCritiqueEngine — Reflexion-inspired evaluation of thinking traces before action.
 *
 * <h2>From the OPENDEV paper (§2.2.6)</h2>
 * At the HIGH thinking level, self-critique is automatically included: a critique model evaluates
 * the initial trace, and the thinking model refines its reasoning with the critique as additional
 * input. An earlier design exposed self-critique as a separate fifth level, but users found the
 * distinction confusing; merging it into HIGH simplified the interface without reducing capability,
 * since users who want deep thinking invariably benefit from critique as well.
 *
 * <h2>Critique model role (§2.2.5)</h2>
 * The CRITIQUE role is specifically designed for self-evaluation, inspired by Reflexion (Shinn et
 * al. 2023) but applied selectively rather than on every turn. Critique is expensive in latency;
 * applying it indiscriminately on every turn proved too slow for routine operations.
 *
 * <h2>What a critique evaluates (paper Appendix K — critique.md template)</h2>
 * <ol>
 *   <li>Logical Coherence — gaps, contradictions, faulty logic</li>
 *   <li>Completeness — overlooked considerations, edge cases, requirements</li>
 *   <li>Assumptions — implicit assumptions that should be validated</li>
 *   <li>Tool/Approach selection — optimal approach? better alternatives?</li>
 *   <li>Risk assessment — unintended consequences addressed?</li>
 * </ol>
 *
 * <h2>Output format</h2>
 * Concise (under 100 words), actionable, specific. If reasoning is sound: "Reasoning is sound."
 * Never re-explains the task or provides a new solution.
 */
@ApplicationScoped
public class SelfCritiqueEngine {

    private static final Logger log = LoggerFactory.getLogger(SelfCritiqueEngine.class);
    private static final int    MAX_CRITIQUE_WORDS = 100;

    @Inject WorkloadModelRouter  modelRouter;
    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig         config;
    @Inject AgentTelemetry        telemetry;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Evaluates a thinking trace and returns critique + refined reasoning.
     *
     * @param task          the original user task
     * @param thinkingTrace the initial reasoning produced by the THINKING model
     * @return a CritiqueResult containing the critique and the refined trace
     */
    public CritiqueResult evaluate(String task, String thinkingTrace) {
        if (thinkingTrace == null || thinkingTrace.isBlank()) {
            return CritiqueResult.skipped("empty thinking trace");
        }

        Instant start = Instant.now();
        log.debug("[critique] evaluating trace for task: {}", truncate(task, 60));
        telemetry.count("critique.evaluate.total");

        // Step 1: Generate critique
        String critiquePrompt = CRITIQUE_TEMPLATE.formatted(task, thinkingTrace);
        String critiqueText;
        try {
            String critiqueModel = modelRouter.modelFor(WorkloadModelRouter.ModelRole.CRITIQUE);
            OrchestratorResult critiqueResult = orchestrator.execute(
                    AgentRequest.builder(critiquePrompt)
                            .model(critiqueModel)
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            critiqueText = critiqueResult.success() ? critiqueResult.answer().strip() : "Reasoning appears sound.";
        } catch (Exception e) {
            log.warn("[critique] critique generation failed: {}", e.getMessage());
            return CritiqueResult.failed(thinkingTrace, e.getMessage());
        }

        // Step 2: Refine the thinking trace with the critique
        String refinedTrace;
        try {
            String refinePrompt = REFINE_TEMPLATE.formatted(task, thinkingTrace, critiqueText);
            OrchestratorResult refineResult = orchestrator.execute(
                    AgentRequest.builder(refinePrompt)
                            .model(modelRouter.modelFor(WorkloadModelRouter.ModelRole.THINKING))
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            refinedTrace = refineResult.success() ? refineResult.answer().strip() : thinkingTrace;
        } catch (Exception e) {
            log.warn("[critique] refinement failed: {}", e.getMessage());
            refinedTrace = thinkingTrace; // fallback: use original trace
        }

        Duration elapsed = Duration.between(start, Instant.now());
        boolean improved = !refinedTrace.equals(thinkingTrace) &&
                           !critiqueText.toLowerCase().contains("reasoning is sound");

        log.debug("[critique] done in {}ms, improved={}", elapsed.toMillis(), improved);
        telemetry.count(improved ? "critique.improved" : "critique.unchanged");
        telemetry.recordLatency("critique.latency", elapsed.toMillis());

        return new CritiqueResult(true, false, null,
                critiqueText, thinkingTrace, refinedTrace, improved, elapsed);
    }

    /**
     * Returns whether the critique engine should be invoked for the given thinking depth.
     * Per paper §2.2.6: HIGH depth automatically includes self-critique.
     */
    public boolean shouldCritique(WorkloadModelRouter.ThinkingDepth depth) {
        return depth == WorkloadModelRouter.ThinkingDepth.HIGH && modelRouter.isCritiqueEnabled();
    }

    // ── Templates (from paper Appendix K — critique.md) ───────────────────

    private static final String CRITIQUE_TEMPLATE = """
            You are a reasoning critic for an AI software engineering assistant.
            Analyze this thinking trace and provide constructive feedback.
            
            Task: %s
            
            Thinking trace:
            %s
            
            Evaluate for:
            1. Logical Coherence — gaps, contradictions, faulty logic
            2. Completeness — overlooked requirements or edge cases
            3. Assumptions — implicit assumptions that should be validated
            4. Tool/Approach selection — optimal choice? Better alternatives?
            5. Risk assessment — potential issues or unintended consequences?
            
            Output CONCISELY (under 100 words):
            - Focus on actionable improvements only
            - Be specific about what is wrong and how to fix it
            - If reasoning is sound, say exactly: "Reasoning is sound."
            - Do NOT re-explain the task or provide a new solution
            - Do NOT use filler phrases""";

    private static final String REFINE_TEMPLATE = """
            You are a reasoning assistant. Refine your thinking trace based on the critique below.
            
            Task: %s
            
            Original thinking:
            %s
            
            Critique:
            %s
            
            Produce a refined, improved reasoning trace (under 100 words, 1 paragraph).
            Address the critique's specific points. If the critique says "Reasoning is sound",
            return the original thinking unchanged.""";

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record CritiqueResult(
            boolean  success,
            boolean  skipped,
            String   skipReason,
            String   critiqueText,
            String   originalTrace,
            String   refinedTrace,
            boolean  improved,
            Duration elapsed
    ) {
        static CritiqueResult skipped(String reason) {
            return new CritiqueResult(false, true, reason, "", "", "", false, Duration.ZERO);
        }
        static CritiqueResult failed(String original, String error) {
            return new CritiqueResult(false, false, error, "", original, original, false, Duration.ZERO);
        }
        public String effectiveTrace() {
            return (improved && !refinedTrace.isBlank()) ? refinedTrace : originalTrace;
        }
        public String summary() {
            if (skipped)  return "Critique skipped: " + skipReason;
            if (!success) return "Critique failed: " + skipReason;
            return String.format("Critique: %s in %dms", improved ? "IMPROVED" : "unchanged", elapsed.toMillis());
        }
    }
}
