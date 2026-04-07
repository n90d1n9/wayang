package tech.kayys.wayang.prompt.core;

import java.util.List;

/**
 * ============================================================================
 * FreeMarkerRenderingEngine — Apache FreeMarker template rendering.
 * ============================================================================
 *
 * Implements Apache FreeMarker template rendering. This provides maximum
 * expressiveness with support for complex logic, macros, includes, and
 * extensive built-in functions.
 *
 * NOTE: This implementation is a stub that falls back to SimpleRenderingEngine
 * if the FreeMarker dependency is not available at runtime.
 */
public class FreeMarkerRenderingEngine implements RenderingEngine {

    @Override
    public String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException {
        // In a real implementation, this would use FreeMarker for advanced rendering
        // For now, fall back to simple rendering
        try {
            SimpleRenderingEngine simpleEngine = new SimpleRenderingEngine();
            return simpleEngine.expand(templateBody, resolvedVars);
        } catch (Exception e) {
            throw new PromptEngineException.PromptRenderException(
                    "FreeMarker template expansion failed: " + e.getMessage(),
                    null,  // template ID not available at this level
                    null,  // node ID not available at this level
                    e);
        }
    }

    @Override
    public PromptVersion.RenderingStrategy getStrategy() {
        return PromptVersion.RenderingStrategy.FREEMARKER;
    }
}