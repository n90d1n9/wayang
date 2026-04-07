package tech.kayys.wayang.prompt.core;

import java.util.*;

/**
 * ============================================================================
 * PromptEngineError — top-level structured exception from the engine.
 * ============================================================================
 *
 * Wraps any failure that occurs inside {@link PromptEngine#buildAndRender()}.
 * The {@link ErrorType} enum lets the ErrorHandlerNode route the failure
 * appropriately (e.g., RENDER_FAILURE → SelfHealingNode).
 *
 * {@link #toErrorDetails()} produces a map ready for injection into
 * {@code ErrorPayload.details}.
 */
public class PromptEngineError extends RuntimeException {

    /** Classification of where in the engine pipeline the failure occurred. */
    public enum ErrorType {
        /** Template could not be found or loaded from the registry. */
        RESOLUTION_FAILURE,
        /** A required variable could not be resolved during interpolation. */
        RENDER_FAILURE,
        /** The chain composition step failed (e.g., CEL evaluation error). */
        CHAIN_FAILURE
    }

    private final ErrorType errorType;
    private final String templateId;
    private final String templateVersion;

    public PromptEngineError(ErrorType errorType, String templateId, String templateVersion, String message) {
        super(message);
        this.errorType = errorType;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    public PromptEngineError(ErrorType errorType, String templateId, String templateVersion, String message,
            Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    /**
     * Produces a {@code Map<String, Object>} suitable for
     * {@code ErrorPayload.details}.
     */
    public Map<String, Object> toErrorDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("errorType", errorType.name());
        details.put("templateId", templateId);
        details.put("templateVersion", templateVersion);
        details.put("message", getMessage());
        if (getCause() != null) {
            details.put("cause", getCause().getMessage());
        }
        return Collections.unmodifiableMap(details);
    }
}