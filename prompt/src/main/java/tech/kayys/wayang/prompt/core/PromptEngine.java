package tech.kayys.wayang.prompt.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * PromptEngine — central façade. The ONLY class AgentNodes inject.
 * ============================================================================
 *
 * Orchestrates the full prompt-building pipeline:
 *
 * ┌─────────┐ ┌─────────┐ ┌────────┐ ┌───────┐ ┌───────┐
 * │ RESOLVE │──►│ CHAIN │──►│ RENDER │──►│ AUDIT │──►│OUTPUT │
 * └─────────┘ └─────────┘ └────────┘ └───────┘ └───────┘
 * │ │ │
 * ▼ ▼ ▼
 * Registry PromptChain PromptRenderer
 * (per-ref) (compose + (interpolate +
 * filter) coerce + redact)
 *
 * Error mapping:
 * • {@link PromptRegistry.TemplateNotFoundException}
 * → {@link PromptEngineError} (RESOLUTION_FAILURE)
 * • {@link PromptRenderException}
 * → {@link PromptEngineError} (RENDER_FAILURE)
 * • Any unexpected exception
 * → {@link PromptEngineError} (CHAIN_FAILURE)
 *
 * All {@link PromptEngineError}s expose
 * {@link PromptEngineError#toErrorDetails()}
 * which maps directly into the platform's {@code ErrorPayload.details}.
 * The ErrorHandlerNode can route RENDER_FAILURE to SelfHealingNode, etc.
 *
 * Provenance / Audit:
 * After a successful render the engine logs the *redacted* chain to
 * {@link ProvenanceService} (skeleton). This ensures that sensitive
 * variable values never appear in audit logs.
 *
 * Reactive contract:
 * {@link #buildAndRender(PromptRequest)} returns {@link Uni<RenderedChain>}.
 * The method never blocks; all I/O is delegated to the reactive registry.
 *
 * Usage inside an AgentNode:
 * 
 * <pre>
 *   {@literal @}Inject PromptEngine promptEngine;
 *
 *   Uni&lt;RenderedChain&gt; chain = promptEngine.buildAndRender(
 *       PromptRequest.builder()
 *           .tenantId(context.getTenantId())
 *           .runId(context.getRunId())
 *           .nodeId(context.getNodeId())
 *           .addTemplateRef(TemplateRef.latest("system-persona"))
 *           .addTemplateRef(TemplateRef.pinned("task-prompt", "2.1.0"))
 *           .explicitValues(Map.of("taskDescription", "..."))
 *           .contextValues(context.getContextMap())
 *           .build()
 *   );
 * </pre>
 */
@ApplicationScoped
public class PromptEngine {

    private static final Logger LOG = Logger.getLogger(PromptEngine.class);

    @Inject
    PromptRegistry registry;

    @Inject
    CelConditionEvaluator celEvaluator;

    // PromptRenderer is stateless and lightweight — create once
    private final PromptRenderer renderer = new PromptRenderer();

    // ------------------------------------------------------------------
    // Main entry point
    // ------------------------------------------------------------------

    /**
     * Builds and renders a complete prompt chain from the given request.
     *
     * @param request the fully-populated {@link PromptRequest}
     * @return a {@link Uni} that emits the assembled {@link RenderedChain}
     * @throws PromptEngineError (wrapped in the Uni failure path) on any error
     */
    public Uni<RenderedChain> buildAndRender(PromptRequest request) {
        LOG.infof("PromptEngine.buildAndRender — run=%s node=%s refs=%d",
                request.getRunId(), request.getNodeId(), request.getTemplateRefs().size());

        // ── Step 1: Resolve all template references (reactive fan-out) ──
        return resolveAllTemplates(request)
                .onItem().transformToUni(templates -> {

                    // ── Step 2: Compose + filter via PromptChain ──
                    PromptChain chain = new PromptChain(renderer, celEvaluator);

                    try {
                        RenderedChain rendered = chain.render(
                                templates,
                                request.getExplicitValues(),
                                request.getContextValues());

                        // ── Step 3: Audit (redacted copy) ──
                        logProvenance(request, rendered);

                        return Uni.createFrom().item(rendered);

                    } catch (PromptRenderException ex) {
                        throw new PromptEngineError(
                                PromptEngineError.ErrorType.RENDER_FAILURE,
                                ex.getTemplateId(),
                                ex.getTemplateVersion(),
                                ex.getMessage(),
                                ex);
                    } catch (Exception ex) {
                        throw new PromptEngineError(
                                PromptEngineError.ErrorType.CHAIN_FAILURE,
                                null, null,
                                "Unexpected error during chain composition: " + ex.getMessage(),
                                ex);
                    }
                })
                .onFailure().transform(ex -> {
                    if (ex instanceof PromptEngineError) {
                        return (PromptEngineError) ex;
                    }
                    return new PromptEngineError(
                            PromptEngineError.ErrorType.CHAIN_FAILURE,
                            null, null,
                            "PromptEngine pipeline failure: " + ex.getMessage(),
                            ex);
                });
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Resolves each {@link TemplateRef} in the request sequentially via the
     * registry. Returns a {@link Uni} that emits the complete list in
     * declaration order.
     *
     * Sequential (not parallel) resolution is intentional: the order of
     * templates in the request IS the composition order, and we want
     * deterministic behaviour.
     */
    private Uni<List<PromptTemplate>> resolveAllTemplates(PromptRequest request) {
        Uni<List<PromptTemplate>> result = Uni.createFrom().item(new ArrayList<PromptTemplate>());

        for (TemplateRef ref : request.getTemplateRefs()) {
            result = result.onItem().transformToUni(list -> {
                Uni<PromptTemplate> resolution;

                if (ref.isLatest()) {
                    resolution = registry.resolveLatest(ref.id(), request.getTenantId());
                } else {
                    resolution = registry.resolvePinned(ref.id(), ref.version(), request.getTenantId());
                }

                return resolution
                        .onFailure(PromptRegistry.TemplateNotFoundException.class)
                        .transform(ex -> new PromptEngineError(
                                PromptEngineError.ErrorType.RESOLUTION_FAILURE,
                                ref.id(),
                                ref.version(),
                                ex.getMessage(),
                                ex))
                        .onItem().invoke(template -> list.add(template))
                        .map(ignored -> list);
            });
        }

        return result;
    }

    /**
     * Logs the redacted chain to the provenance store.
     *
     * This is a skeleton — wire the platform's ProvenanceService here.
     * The method intentionally receives the full {@link PromptRequest} so
     * that runId / nodeId / tenantId can be included in the audit event.
     *
     * Audit event structure (maps to AuditPayload):
     * event = "PROMPT_RENDERED"
     * actor = { type: "system" }
     * metadata = {
     * runId, nodeId, tenantId,
     * messageCount,
     * skippedTemplateIds,
     * redactedContent ← the safe copy
     * }
     */
    private void logProvenance(PromptRequest request, RenderedChain rendered) {
        LOG.debugf("PROMPT_RENDERED — run=%s node=%s messages=%d skipped=%s",
                request.getRunId(),
                request.getNodeId(),
                rendered.messageCount(),
                rendered.skippedTemplateIds());

        // TODO: inject ProvenanceService and emit a full AuditPayload event.
        // The redactedContent() is the only content that should appear
        // in the audit store.
    }
}
