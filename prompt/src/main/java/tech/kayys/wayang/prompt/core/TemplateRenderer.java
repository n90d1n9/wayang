package tech.kayys.wayang.prompt.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * TemplateRenderer — orchestrates the full prompt render pipeline.
 * ============================================================================
 *
 * Pipeline stages (executed in order)
 * ------------------------------------
 * 1. Resolve the target PromptVersion from the registry.
 * 2. Resolve all variables via VariableResolver.
 * 3. Delegate template expansion to the appropriate RenderingEngine.
 * 4. Enforce maxContextTokens cap (if set).
 * 5. Compute SHA-256 content hash for audit integrity.
 * 6. Wrap everything into an immutable RenderedPrompt.
 *
 * Error handling
 * --------------
 * Every stage that can fail throws a typed PromptEngineException subclass.
 * The caller (typically an AgentNode) catches these and emits an ErrorPayload
 * on the node's error output port — plugging directly into the platform's
 * Error Channel pattern (Blueprint §1).
 *
 * Modularity
 * ----------
 * The RenderingEngine is looked up via CDI qualifier
 * {@link RenderingEngineQualifier}.
 * The standalone runtime registers only the SimpleRenderingEngine; the full
 * platform registers all three. TemplateRenderer itself has zero direct
 * dependency on any template language library.
 */
@ApplicationScoped
public class TemplateRenderer {

    private static final Logger LOG = Logger.getLogger(TemplateRenderer.class);

    @Inject
    VariableResolver variableResolver;

    @Inject
    RenderingEngineRegistry engineRegistry;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Main entry point. Renders the template identified by
     * {@code ctx.getTemplateId()} (and optionally {@code ctx.getVersionOverride()})
     * into a fully-expanded RenderedPrompt.
     *
     * @param version the resolved PromptVersion to render
     * @param ctx     the runtime context carrying all variable sources
     * @return a Uni emitting the rendered prompt
     * @throws PromptEngineException on any validation or rendering failure
     */
    public Uni<RenderedPrompt> render(PromptVersion version, PromptRenderContext ctx) {
        LOG.debugf("Starting render: template='%s', version='%s', node='%s'",
                ctx.getTemplateId(), version.getVersion(), ctx.getNodeId());

        // Stage 1: Resolve variables
        return variableResolver
                .resolve(ctx.resolveAny("__variableDefinitions__") != null
                        ? (List<PromptVariableDefinition>) ctx.resolveAny("__variableDefinitions__")
                        : List.of(), ctx)

                // Stage 2: Expand template body
                .onItem().transformToUni(resolvedVars -> expandTemplate(version, resolvedVars, ctx))

                // Stage 3: Enforce token cap & build final artifact
                .onItem().transform(expanded -> assemblePrompt(version, expanded, ctx));
    }

    /**
     * Overload that accepts pre-resolved variables directly.
     * Used when the caller has already performed resolution (e.g. in tests
     * or when the Engine pre-resolves for performance).
     */
    public Uni<RenderedPrompt> renderWithVariables(
            PromptVersion version,
            List<PromptVariableValue> resolvedVars,
            PromptRenderContext ctx) {

        return expandTemplate(version, resolvedVars, ctx)
                .onItem().transform(expanded -> assemblePrompt(version, expanded, ctx));
    }

    // -----------------------------------------------------------------------
    // Stage 2: Template expansion
    // -----------------------------------------------------------------------

    /**
     * Delegates to the correct RenderingEngine based on the version's strategy.
     */
    private Uni<String> expandTemplate(
            PromptVersion version,
            List<PromptVariableValue> resolvedVars,
            PromptRenderContext ctx) {

        RenderingEngine engine = engineRegistry.forStrategy(version.getRenderingStrategy());

        return Uni.createFrom().deferred(() -> {
            try {
                String expanded = engine.expand(version.getTemplateBody(), resolvedVars);
                return Uni.createFrom().item(expanded);
            } catch (Exception e) {
                throw new PromptEngineException.PromptRenderException(
                        "Template expansion failed for '%s' v%s: %s"
                                .formatted(ctx.getTemplateId(), version.getVersion(), e.getMessage()),
                        ctx.getTemplateId(),
                        ctx.getNodeId(),
                        e);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Stage 3: Token cap enforcement & artifact assembly
    // -----------------------------------------------------------------------

    /**
     * Enforces maxContextTokens and assembles the final RenderedPrompt.
     */
    private RenderedPrompt assemblePrompt(
            PromptVersion version,
            String expandedBody,
            PromptRenderContext ctx) {

        // Estimate input tokens (simple heuristic: ~4 chars per token).
        // A more accurate estimator can be injected in production via CDI.
        int estimatedTokens = estimateTokens(expandedBody, version.getSystemPrompt());

        // Enforce cap
        if (version.getMaxContextTokens() != null && estimatedTokens > version.getMaxContextTokens()) {
            throw new PromptEngineException.PromptTokenLimitExceededException(
                    ctx.getTemplateId(),
                    ctx.getNodeId(),
                    estimatedTokens,
                    version.getMaxContextTokens());
        }

        // Compute content hash for audit integrity
        String contentHash = computeContentHash(version.getSystemPrompt(), expandedBody);

        LOG.debugf("Render complete: template='%s', v='%s', userLen=%d, tokens~%d, hash='%s'",
                ctx.getTemplateId(), version.getVersion(),
                expandedBody.length(), estimatedTokens, contentHash);

        // Assemble — note: resolvedVars are NOT stored here to avoid re-fetching;
        // the caller must pass them if provenance needs them (see renderWithVariables).
        return new RenderedPrompt(
                UUID.randomUUID().toString(), // rendererId
                ctx.getTemplateId(),
                version.getVersion(),
                version.getSystemPrompt(), // may be null
                expandedBody,
                List.of(), // resolvedVariables populated by caller if needed
                version.getMaxOutputTokens(),
                version.getMaxContextTokens(),
                estimatedTokens,
                contentHash,
                Instant.now(),
                ctx.getRunId(),
                ctx.getNodeId(),
                ctx.getTenantId());
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /**
     * SHA-256 hash of the concatenated system + user messages.
     * Used by the audit layer to detect post-creation tampering.
     */
    private static String computeContentHash(String systemMsg, String userMsg) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (systemMsg != null) {
                digest.update(systemMsg.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            digest.update(userMsg.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();

            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append("%02x".formatted(b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this should never happen.
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Simple token estimator: ~4 characters per token (GPT-style heuristic).
     * In production, replace with a proper tokeniser via CDI.
     */
    private static int estimateTokens(String userMsg, String systemMsg) {
        int totalChars = userMsg.length();
        if (systemMsg != null) {
            totalChars += systemMsg.length();
        }
        return totalChars / 4;
    }
}
