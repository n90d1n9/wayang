package tech.kayys.wayang.prompt.core;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Jinja2RenderingEngine — Jinja2-compatible template rendering using Pebble.
 * ============================================================================
 *
 * Implements Jinja2-compatible template rendering using the Pebble template
 * engine. This provides support for loops, conditionals, filters, and other
 * advanced templating features while maintaining compatibility with Jinja2 syntax.
 *
 * This engine evaluates literal template bodies from the prompt registry.
 */
@ApplicationScoped
public class Jinja2RenderingEngine implements RenderingEngine {

    private final PebbleEngine engine;

    public Jinja2RenderingEngine() {
        engine = new PebbleEngine.Builder()
                .autoEscaping(false)
                .cacheActive(false)
                .strictVariables(false)
                .build();
    }

    @Override
    public String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException {
        try {
            PebbleTemplate template = engine.getLiteralTemplate(templateBody);
            StringWriter writer = new StringWriter();
            template.evaluate(writer, variableMap(resolvedVars));
            return writer.toString();
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

    private static Map<String, Object> variableMap(List<PromptVariableValue> resolvedVars) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (PromptVariableValue var : resolvedVars) {
            if (var.getValue() != null) {
                variables.put(var.getName(), var.getValue());
            }
        }
        return variables;
    }
}
