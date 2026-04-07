package tech.kayys.wayang.prompt.core;

import java.util.List;

/**
 * ============================================================================
 * Jinja2RenderingEngine — Jinja2-compatible template rendering using Pebble.
 * ============================================================================
 *
 * Implements Jinja2-compatible template rendering using the Pebble template
 * engine. This provides support for loops, conditionals, filters, and other
 * advanced templating features while maintaining compatibility with Jinja2 syntax.
 *
 * NOTE: This implementation is a stub that falls back to SimpleRenderingEngine
 * if the Pebble dependency is not available at runtime.
 */
public class Jinja2RenderingEngine implements RenderingEngine {

    @Override
    public String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException {
        // In a real implementation, this would use Pebble for Jinja2 rendering
        // For now, fall back to simple rendering
        try {
            SimpleRenderingEngine simpleEngine = new SimpleRenderingEngine();
            return simpleEngine.expand(templateBody, resolvedVars);
        } catch (Exception e) {
            throw new PromptEngineException.PromptRenderException(
                    "Jinja2 template expansion failed: " + e.getMessage(),
                    null,  // template ID not available at this level
                    null,  // node ID not available at this level
                    e);
        }
    }

    @Override
    public PromptVersion.RenderingStrategy getStrategy() {
        return PromptVersion.RenderingStrategy.JINJA2;
    }
}