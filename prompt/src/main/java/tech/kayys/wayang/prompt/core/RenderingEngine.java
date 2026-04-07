package tech.kayys.wayang.prompt.core;

import java.util.List;

/**
 * ============================================================================
 * RenderingEngine — abstraction for different template rendering strategies.
 * ============================================================================
 *
 * This interface abstracts the template rendering process, allowing the platform
 * to support multiple templating engines (Simple, Jinja2, FreeMarker, etc.)
 * without tight coupling to specific implementations.
 */
public interface RenderingEngine {
    
    /**
     * Expands the template body with the provided variable values.
     *
     * @param templateBody The template body to expand
     * @param resolvedVars The resolved variable values
     * @return The expanded template body
     * @throws PromptEngineException.PromptRenderException if rendering fails
     */
    String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException;
    
    /**
     * Gets the rendering strategy this engine handles.
     *
     * @return The rendering strategy
     */
    PromptVersion.RenderingStrategy getStrategy();
}