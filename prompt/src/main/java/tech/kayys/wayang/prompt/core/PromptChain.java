package tech.kayys.wayang.prompt.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ============================================================================
 * PromptChain — multi-template composition and ordering layer.
 * ============================================================================
 *
 * An AgentNode typically needs more than one template to build a complete
 * LLM prompt: a system-persona template, a task-specific template, maybe a
 * few-shot example template. PromptChain assembles them into a single,
 * correctly-ordered {@link RenderedChain}.
 *
 * Ordering rules (mirroring how OpenAI / Anthropic APIs expect messages[]):
 * 1. All SYSTEM templates are *merged* into a single message at position 0,
 * separated by {@code \n\n}. Merge order follows the order they appear
 * in the input list.
 * 2. USER and ASSISTANT templates follow in their original declaration
 * order (no re-sorting). This preserves few-shot conversation flow.
 *
 * CEL condition filtering:
 * • Each template may carry an optional {@code condition} (a CEL expression).
 * • Before rendering, the chain evaluates the condition via the injected
 * {@link ConditionEvaluator}. If it returns {@code false} the template
 * is skipped entirely and its ID is recorded in
 * {@link RenderedChain#skippedTemplateIds()}.
 * • Templates with no condition are always included.
 *
 * Thread safety:
 * PromptChain is stateless once constructed. The same instance can be
 * reused across concurrent render calls.
 *
 * Standalone compatibility:
 * The {@link ConditionEvaluator} is a strategy interface. The platform
 * wires a CEL-Java backed implementation; standalone runtimes can inject
 * {@link NoOpConditionEvaluator} (always {@code true}).
 */
public final class PromptChain {

    private final PromptRenderer renderer;
    private final ConditionEvaluator conditionEvaluator;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------
    public PromptChain(PromptRenderer renderer, ConditionEvaluator conditionEvaluator) {
        this.renderer = Objects.requireNonNull(renderer);
        this.conditionEvaluator = conditionEvaluator != null
                ? conditionEvaluator
                : NoOpConditionEvaluator.INSTANCE;
    }

    /** Convenience — no condition evaluator (standalone). */
    public PromptChain(PromptRenderer renderer) {
        this(renderer, NoOpConditionEvaluator.INSTANCE);
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------
    /**
     * Assembles and renders a chain of templates into an ordered
     * {@link RenderedChain}.
     *
     * @param templates      the ordered list of templates to compose
     * @param explicitValues values supplied by the calling node
     * @param contextValues  values supplied by the workflow context
     * @return an immutable {@link RenderedChain}
     * @throws PromptRenderException if any required variable in any included
     *                               template cannot be resolved
     */
    public RenderedChain render(
            List<PromptTemplate> templates,
            Map<String, Object> explicitValues,
            Map<String, Object> contextValues) throws PromptRenderException {

        Objects.requireNonNull(templates, "templates must not be null");

        // ── Phase 1: filter by CEL condition ─────────────────────────
        List<PromptTemplate> included = new ArrayList<>();
        Set<String> skipped = new LinkedHashSet<>();

        for (PromptTemplate t : templates) {
            if (t.hasCondition()) {
                boolean passes = conditionEvaluator.evaluate(
                        t.getCondition(), explicitValues, contextValues);
                if (!passes) {
                    skipped.add(t.getTemplateId());
                    continue;
                }
            }
            included.add(t);
        }

        // ── Phase 2: partition into SYSTEM vs rest ───────────────────
        List<PromptTemplate> systemTemplates = included.stream()
                .filter(t -> t.getRole() == PromptRole.SYSTEM)
                .toList();
        List<PromptTemplate> restTemplates = included.stream()
                .filter(t -> t.getRole() != PromptRole.SYSTEM)
                .toList();

        // ── Phase 3: render each template ────────────────────────────
        List<RenderResult> messages = new ArrayList<>();

        // 3a. Merge all SYSTEM templates into one combined message
        if (!systemTemplates.isEmpty()) {
            RenderResult merged = renderAndMergeSystems(systemTemplates, explicitValues, contextValues);
            messages.add(merged);
        }

        // 3b. Render USER / ASSISTANT in declaration order
        for (PromptTemplate t : restTemplates) {
            messages.add(renderer.render(t, explicitValues, contextValues));
        }

        return new RenderedChain(messages, skipped);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Renders each SYSTEM template independently, then merges their
     * {@code content} and {@code redactedContent} with {@code \n\n}.
     * Resolution sources and resolved values are union-ed (last wins on
     * key collision — later SYSTEM templates override earlier ones).
     */
    private RenderResult renderAndMergeSystems(
            List<PromptTemplate> systemTemplates,
            Map<String, Object> explicitValues,
            Map<String, Object> contextValues) throws PromptRenderException {

        StringBuilder liveBuilder = new StringBuilder();
        StringBuilder redactedBuilder = new StringBuilder();
        Map<String, RenderResult.ResolutionSource> mergedSources = new LinkedHashMap<>();
        Map<String, Object> mergedValues = new LinkedHashMap<>();

        for (PromptTemplate t : systemTemplates) {
            RenderResult r = renderer.render(t, explicitValues, contextValues);

            if (liveBuilder.length() > 0)
                liveBuilder.append("\n\n");
            liveBuilder.append(r.content());

            if (redactedBuilder.length() > 0)
                redactedBuilder.append("\n\n");
            redactedBuilder.append(r.redactedContent());

            mergedSources.putAll(r.resolutionSources());
            mergedValues.putAll(r.resolvedValues());
        }

        return new RenderResult(
                liveBuilder.toString(),
                redactedBuilder.toString(),
                mergedSources,
                mergedValues);
    }

    // ============================================================
    // ConditionEvaluator (strategy interface)
    // ============================================================
    /**
     * Strategy for evaluating CEL conditions on templates.
     *
     * The platform wires a CEL-Java backed implementation.
     * Standalone runtimes inject {@link NoOpConditionEvaluator}.
     *
     * @param condition      the CEL expression string from the template
     * @param explicitValues the explicit variable map (bound as {@code explicit})
     * @param contextValues  the context variable map (bound as {@code context})
     * @return {@code true} when the template should be included
     */
    @FunctionalInterface
    public interface ConditionEvaluator {
        boolean evaluate(
                String condition,
                Map<String, Object> explicitValues,
                Map<String, Object> contextValues);
    }

    /**
     * No-op evaluator — always returns {@code true}.
     * Safe default for standalone runtimes that do not have CEL available.
     */
    public static final class NoOpConditionEvaluator implements ConditionEvaluator {
        /** Singleton instance. */
        public static final NoOpConditionEvaluator INSTANCE = new NoOpConditionEvaluator();

        private NoOpConditionEvaluator() {
        }

        @Override
        public boolean evaluate(String condition,
                Map<String, Object> explicitValues,
                Map<String, Object> contextValues) {
            return true; // include everything
        }
    }
}
