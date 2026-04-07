package tech.kayys.wayang.prompt.core;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * RenderedPrompt — the fully-expanded, ready-to-send prompt artifact.
 * ============================================================================
 *
 * This is the *output* of the Prompt Engine's render pipeline. It contains
 * everything the LLM adapter needs to make a completion call, plus everything
 * the Provenance layer needs to reconstruct this exact call later.
 *
 * Immutability guarantee
 * ----------------------
 * Once created, a RenderedPrompt is never mutated. It is passed by reference
 * through the AgentNode → LLM Adapter → Provenance chain. The
 * {@link #contentHash} field enables the audit layer to detect any tampering
 * that might occur between creation and storage.
 *
 * Relationship to the platform
 * ----------------------------
 * - Created by {@code TemplateRenderer} after variable resolution.
 * - Consumed by the LLM adapter (langchain4j {@code ChatModel}).
 * - Snapshotted by ProvenanceService as part of every agent execution record.
 * - If rendering fails, an ErrorPayload with type {@code LLMError} or
 * {@code ValidationError} is emitted on the node's error output port
 * — consistent with the Error Channel pattern from the blueprint.
 */
public final class RenderedPrompt {

    /** Unique render-invocation ID (UUID). Useful for distributed tracing. */
    private final String rendererId;

    /** The template ID that produced this prompt. */
    private final String templateId;

    /** The specific version string of the template used. */
    private final String templateVersion;

    /**
     * The system message sent to the LLM.
     * May be null if neither the version nor the agent defines one.
     */
    private final String systemMessage;

    /** The fully-expanded user message — this is what the LLM "sees". */
    private final String userMessage;

    /**
     * Ordered snapshot of all variable values that were interpolated.
     * Sensitive values are already replaced with {@code <REDACTED>} in this list.
     * This enables full provenance without leaking secrets.
     */
    private final List<PromptVariableValue> resolvedVariables;

    /**
     * Maximum tokens the LLM may generate. Null = model default.
     * Copied from the PromptVersion at render time.
     */
    private final Integer maxOutputTokens;

    /**
     * Hard cap on input-token size of this prompt. Null = no explicit cap.
     * The renderer enforces this *before* returning; if the expanded prompt
     * exceeds the cap, a ValidationError is emitted.
     */
    private final Integer maxContextTokens;

    /** Approximate input-token count (estimated by the renderer). */
    private final Integer estimatedInputTokens;

    /**
     * SHA-256 hash of {@code systemMessage + "\n" + userMessage}.
     * Used by the audit layer to detect post-creation tampering.
     */
    private final String contentHash;

    /** Wall-clock timestamp when this RenderedPrompt was created. */
    private final Instant renderedAt;

    /** The run ID of the workflow execution that triggered this render. */
    private final String runId;

    /** The node ID within that run. */
    private final String nodeId;

    /** Tenant that owns this render invocation. */
    private final String tenantId;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public RenderedPrompt(
            String rendererId,
            String templateId,
            String templateVersion,
            String systemMessage,
            String userMessage,
            List<PromptVariableValue> resolvedVariables,
            Integer maxOutputTokens,
            Integer maxContextTokens,
            Integer estimatedInputTokens,
            String contentHash,
            Instant renderedAt,
            String runId,
            String nodeId,
            String tenantId) {

        Objects.requireNonNull(rendererId, "rendererId must not be null");
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(userMessage, "userMessage must not be null");
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        Objects.requireNonNull(renderedAt, "renderedAt must not be null");

        this.rendererId = rendererId;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        this.resolvedVariables = Collections.unmodifiableList(
                resolvedVariables != null ? List.copyOf(resolvedVariables) : List.of());
        this.maxOutputTokens = maxOutputTokens;
        this.maxContextTokens = maxContextTokens;
        this.estimatedInputTokens = estimatedInputTokens;
        this.contentHash = contentHash;
        this.renderedAt = renderedAt;
        this.runId = runId;
        this.nodeId = nodeId;
        this.tenantId = tenantId;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getRendererId() {
        return rendererId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public List<PromptVariableValue> getResolvedVariables() {
        return resolvedVariables;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public Integer getMaxContextTokens() {
        return maxContextTokens;
    }

    public Integer getEstimatedInputTokens() {
        return estimatedInputTokens;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getRenderedAt() {
        return renderedAt;
    }

    public String getRunId() {
        return runId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getTenantId() {
        return tenantId;
    }

    /** Convenience: returns true when a system message is present. */
    public boolean hasSystemMessage() {
        return systemMessage != null && !systemMessage.isBlank();
    }

    // -----------------------------------------------------------------------
    // Object contract
    // -----------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RenderedPrompt that))
            return false;
        return Objects.equals(rendererId, that.rendererId)
                && Objects.equals(contentHash, that.contentHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rendererId, contentHash);
    }

    @Override
    public String toString() {
        return ("RenderedPrompt{id='%s', template='%s@%s', run='%s', node='%s', "
                + "userLen=%d, hash='%s'}").formatted(
                        rendererId, templateId, templateVersion, runId, nodeId,
                        userMessage.length(), contentHash);
    }
}
