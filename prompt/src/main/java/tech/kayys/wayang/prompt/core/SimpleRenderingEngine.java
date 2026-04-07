package tech.kayys.wayang.prompt.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * SimpleRenderingEngine — basic {{placeholder}} interpolation engine.
 * ============================================================================
 *
 * Implements the simple placeholder replacement strategy using {{variable}}
 * syntax. This is the minimal dependency option suitable for standalone runtimes.
 */
public class SimpleRenderingEngine implements RenderingEngine {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");
    
    @Override
    public String expand(String templateBody, List<PromptVariableValue> resolvedVars) throws PromptEngineException.PromptRenderException {
        // Convert resolved variables to a map for easy lookup
        Map<String, Object> variableMap = new HashMap<>();
        for (PromptVariableValue var : resolvedVars) {
            // Only add non-null values to the map
            if (var.getValue() != null) {
                variableMap.put(var.getName(), var.getValue());
            }
        }
        
        // Perform placeholder replacement
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateBody);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            
            if (variableMap.containsKey(variableName)) {
                Object value = variableMap.get(variableName);
                // Convert value to string for insertion
                String stringValue = value != null ? value.toString() : "";
                matcher.appendReplacement(result, Matcher.quoteReplacement(stringValue));
            } else {
                // If variable is not found, leave the placeholder as is
                // This maintains backward compatibility with the original behavior
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    @Override
    public PromptVersion.RenderingStrategy getStrategy() {
        return PromptVersion.RenderingStrategy.SIMPLE;
    }
}