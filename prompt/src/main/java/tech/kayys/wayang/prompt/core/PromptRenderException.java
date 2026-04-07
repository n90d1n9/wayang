package tech.kayys.wayang.prompt.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * PromptRenderException — structured failure from the render pipeline.
 * ============================================================================
 *
 * Thrown by {@link PromptRenderer} when a required variable cannot be
 * resolved. Carries enough context to map directly onto the platform's
 * {@code ErrorPayload} via {@link #toErrorDetails()}.
 *
 * This is a *checked* exception so that callers are forced to handle it
 * explicitly (either catch or propagate as an {@code ErrorPayload}).
 */
public class PromptRenderException extends Exception {

    private final String variableName;
    private final String templateId;
    private final String templateVersion;

    /**
     * @param variableName    the variable that could not be resolved
     * @param templateId      the template that owns the placeholder
     * @param templateVersion the version of that template
     * @param message         human-readable description
     */
    public PromptRenderException(
            String variableName,
            String templateId,
            String templateVersion,
            String message) {
        super(message);
        this.variableName = variableName;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    /**
     * Produces a {@code Map<String, Object>} suitable for injecting into
     * {@code ErrorPayload.details}. Consumers can forward this directly
     * to the ErrorHandlerNode without further transformation.
     */
    public Map<String, Object> toErrorDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("variableName", variableName);
        details.put("templateId", templateId);
        details.put("templateVersion", templateVersion);
        details.put("message", getMessage());
        return Collections.unmodifiableMap(details);
    }
}