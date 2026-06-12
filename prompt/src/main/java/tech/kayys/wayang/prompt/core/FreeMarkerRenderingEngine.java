package tech.kayys.wayang.prompt.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.StringWriter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * FreeMarkerRenderingEngine — Apache FreeMarker template rendering.
 * ============================================================================
 *
 * Implements Apache FreeMarker template rendering. This provides maximum
 * expressiveness with support for complex logic, macros, includes, and
 * extensive built-in functions.
 *
 * This engine is intentionally string-backed: prompt templates are supplied by
 * the registry and not loaded from the filesystem.
 */
@ApplicationScoped
public class FreeMarkerRenderingEngine implements RenderingEngine {

    private final Configuration configuration;

    public FreeMarkerRenderingEngine() {
        configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
    }

    @Override
    public String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException {
        try {
            Template template = new Template("wayang-prompt", templateBody, configuration);
            StringWriter writer = new StringWriter();
            template.process(variableMap(resolvedVars), writer);
            return writer.toString();
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
