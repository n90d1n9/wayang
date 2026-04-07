package tech.kayys.wayang.prompt.core;

/**
 * ============================================================================
 * PromptEngineException — root of the Prompt Engine exception hierarchy.
 * ============================================================================
 *
 * Every exception thrown by the Prompt Engine maps to a specific
 * {@code ErrorPayload.type} as defined in the Core Schema. This mapping is
 * declared in the Javadoc of each subclass so that the ErrorHandlerNode's
 * CEL rules can route errors deterministically without inspecting the
 * Java exception class.
 *
 * Mapping table
 * -------------
 * | Exception class | ErrorPayload.type | retryable |
 * |--------------------------------------|--------------------|-----------|
 * | PromptTemplateNotFoundException | UnknownError | false |
 * | PromptValidationException | ValidationError | false |
 * | PromptRenderException | LLMError | true |
 * | PromptTokenLimitExceededException | ValidationError | false |
 * | PromptEngineException (catch-all) | UnknownError | true |
 */
public class PromptEngineException extends RuntimeException {

    /**
     * The template ID involved in the failure (may be null for registry-level
     * errors).
     */
    private final String templateId;

    /** The node that triggered the render (for provenance). */
    private final String originNode;

    /** Maps to ErrorPayload.type — used by the ErrorHandlerNode for routing. */
    private final ErrorCategory errorCategory;

    /** Whether the Engine should retry this operation automatically. */
    private final boolean retryable;

    public PromptEngineException(String message, String templateId, String originNode,
            ErrorCategory errorCategory, boolean retryable) {
        super(message);
        this.templateId = templateId;
        this.originNode = originNode;
        this.errorCategory = errorCategory;
        this.retryable = retryable;
    }

    public PromptEngineException(String message, String templateId, String originNode,
            ErrorCategory errorCategory, boolean retryable, Throwable cause) {
        super(message, cause);
        this.templateId = templateId;
        this.originNode = originNode;
        this.errorCategory = errorCategory;
        this.retryable = retryable;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getOriginNode() {
        return originNode;
    }

    public ErrorCategory getErrorCategory() {
        return errorCategory;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Maps to ErrorPayload.type values from the Core Schema's ErrorPayload
     * definition.
     */
    public enum ErrorCategory {
        VALIDATION_ERROR, // → "ValidationError"
        LLM_ERROR, // → "LLMError"
        UNKNOWN_ERROR // → "UnknownError"
    }

    // -----------------------------------------------------------------------
    // Concrete subclasses
    // -----------------------------------------------------------------------

    /**
     * Thrown when the requested template ID does not exist in the registry
     * for the given tenant.
     *
     * ErrorPayload.type → UnknownError | retryable → false
     */
    public static class PromptTemplateNotFoundException extends PromptEngineException {
        public PromptTemplateNotFoundException(String templateId, String tenantId, String originNode) {
            super("PromptTemplate not found: id='%s', tenant='%s'".formatted(templateId, tenantId),
                    templateId, originNode, ErrorCategory.UNKNOWN_ERROR, false);
        }
    }

    /**
     * Thrown when a required variable has no value, a variable value exceeds
     * its maxLength, or the template body fails schema validation.
     *
     * ErrorPayload.type → ValidationError | retryable → false
     *
     * The SelfHealingNode can often auto-correct ValidationErrors by asking
     * the LLM to produce a corrected input — see the blueprint's §3.3.
     */
    public static class PromptValidationException extends PromptEngineException {
        /** The specific variable that failed validation, if applicable. */
        private final String variableName;

        public PromptValidationException(String message, String templateId,
                String originNode, String variableName) {
            super(message, templateId, originNode, ErrorCategory.VALIDATION_ERROR, false);
            this.variableName = variableName;
        }

        public String getVariableName() {
            return variableName;
        }
    }

    /**
     * Thrown when the rendering engine itself fails (e.g. malformed template
     * syntax, Jinja2 / FreeMarker parse error). This is retryable because
     * the underlying cause may be a transient resource issue (e.g. template
     * engine classloader not yet warmed up).
     *
     * ErrorPayload.type → LLMError | retryable → true
     */
    public static class PromptRenderException extends PromptEngineException {
        public PromptRenderException(String message, String templateId,
                String originNode, Throwable cause) {
            super(message, templateId, originNode, ErrorCategory.LLM_ERROR, true, cause);
        }
    }

    /**
     * Thrown when the expanded prompt exceeds the maxContextTokens cap
     * declared in the PromptVersion. This is *not* retryable because
     * re-rendering the same template with the same inputs will produce
     * the same oversized output.
     *
     * ErrorPayload.type → ValidationError | retryable → false
     *
     * Resolution path: the ErrorHandlerNode routes to SelfHealingNode, which
     * can ask the LLM to summarise the offending RAG chunks to fit the cap.
     */
    public static class PromptTokenLimitExceededException extends PromptEngineException {
        private final int actualTokens;
        private final int limitTokens;

        public PromptTokenLimitExceededException(String templateId, String originNode,
                int actualTokens, int limitTokens) {
            super("Prompt token limit exceeded: actual=%d, limit=%d".formatted(actualTokens, limitTokens),
                    templateId, originNode, ErrorCategory.VALIDATION_ERROR, false);
            this.actualTokens = actualTokens;
            this.limitTokens = limitTokens;
        }

        public int getActualTokens() {
            return actualTokens;
        }

        public int getLimitTokens() {
            return limitTokens;
        }
    }
}