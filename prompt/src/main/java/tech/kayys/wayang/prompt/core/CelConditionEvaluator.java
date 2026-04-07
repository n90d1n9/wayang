package tech.kayys.wayang.prompt.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * CelConditionEvaluator — production CEL strategy for PromptChain.
 * ============================================================================
 *
 * Evaluates CEL (Common Expression Language) conditions attached to
 * {@link PromptTemplate}s. The platform wires this bean into
 * {@link PromptChain} via CDI injection.
 *
 * Variable bindings exposed to the CEL expression:
 * • {@code explicit} – the Map<String,Object> of explicit values.
 * • {@code context} – the Map<String,Object> of context values.
 * • Every key from both maps is also bound as a top-level variable for
 * convenience (e.g., {@code taskMode} instead of {@code explicit.taskMode}).
 * In case of key collision, {@code explicit} wins.
 *
 * Examples of valid conditions:
 * • {@code "has(context.ragResult)"}
 * • {@code "explicit.taskMode == 'advanced'"}
 * • {@code "context.confidenceScore > 0.8"}
 *
 * Integration note:
 * The full CEL-Java library (google/cel-java) should be added as a
 * dependency. This skeleton implements a lightweight pattern-matcher for
 * the most common expression forms so that the module can be tested
 * without that dependency. Replace {@link #evaluateInternal()} with the
 * real CEL program compilation and evaluation when the dependency is
 * available.
 *
 * Standalone compatibility:
 * Standalone runtimes that cannot carry CEL should inject
 * {@link PromptChain.NoOpConditionEvaluator} instead (always {@code true}).
 */
@ApplicationScoped
public class CelConditionEvaluator implements PromptChain.ConditionEvaluator {

    private static final Logger LOG = Logger.getLogger(CelConditionEvaluator.class);

    @Override
    public boolean evaluate(
            String condition,
            Map<String, Object> explicitValues,
            Map<String, Object> contextValues) {
        if (condition == null || condition.isBlank()) {
            return true; // no condition = always include
        }

        // Build unified variable map for the evaluator
        Map<String, Object> bindings = new HashMap<>();
        if (contextValues != null)
            bindings.putAll(contextValues);
        if (explicitValues != null)
            bindings.putAll(explicitValues); // explicit wins on collision

        // Also expose the sub-maps themselves
        bindings.put("explicit", explicitValues != null ? explicitValues : Map.of());
        bindings.put("context", contextValues != null ? contextValues : Map.of());

        try {
            return evaluateInternal(condition, bindings);
        } catch (Exception ex) {
            LOG.warnf("CEL evaluation failed for condition '%s': %s — defaulting to true", condition, ex.getMessage());
            return true; // fail-open: include the template rather than silently drop it
        }
    }

    // ------------------------------------------------------------------
    // Internal evaluation (lightweight pattern matcher)
    // ------------------------------------------------------------------
    /**
     * Lightweight CEL evaluator for common expression patterns.
     *
     * Supported forms:
     * • {@code "has(map.key)"} → checks key presence
     * • {@code "map.key == 'literal'"} → string equality
     * • {@code "map.key != 'literal'"} → string inequality
     * • {@code "map.key > number"} → numeric greater-than
     * • {@code "map.key < number"} → numeric less-than
     * • {@code "true"} / {@code "false"} → literal boolean
     *
     * Replace this method body with real cel-java compilation when available.
     */
    private boolean evaluateInternal(String condition, Map<String, Object> bindings) {
        String expr = condition.trim();

        // Literal boolean
        if ("true".equalsIgnoreCase(expr))
            return true;
        if ("false".equalsIgnoreCase(expr))
            return false;

        // has(map.key) — presence check
        if (expr.startsWith("has(") && expr.endsWith(")")) {
            String path = expr.substring(4, expr.length() - 1).trim();
            Object value = resolvePath(path, bindings);
            return value != null;
        }

        // Equality / inequality: map.key == 'value' or map.key != 'value'
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            Object left = resolvePath(parts[0].trim(), bindings);
            String right = stripQuotes(parts[1].trim());
            return right.equals(left != null ? left.toString() : null);
        }
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            Object left = resolvePath(parts[0].trim(), bindings);
            String right = stripQuotes(parts[1].trim());
            return !right.equals(left != null ? left.toString() : null);
        }

        // Numeric comparisons: map.key > N or map.key < N
        if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            return compareNumeric(parts[0].trim(), parts[1].trim(), bindings) > 0;
        }
        if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            return compareNumeric(parts[0].trim(), parts[1].trim(), bindings) < 0;
        }

        // Unrecognised expression — log warning, default to true (fail-open)
        LOG.warnf("Unrecognised CEL pattern: '%s' — defaulting to true", expr);
        return true;
    }

    /**
     * Resolves a dotted path like {@code "explicit.taskMode"} or
     * {@code "context.ragResult"} against the bindings map.
     */
    private static Object resolvePath(String path, Map<String, Object> bindings) {
        String[] segments = path.split("\\.", 2);
        Object root = bindings.get(segments[0]);
        if (segments.length == 1 || root == null)
            return root;

        // Second segment — root must be a Map
        if (root instanceof Map map) {
            return map.get(segments[1]);
        }
        return null;
    }

    /** Strips surrounding single or double quotes from a literal. */
    private static String stripQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * Resolves left path and right literal, returns comparison as int (-1, 0, 1).
     */
    private static int compareNumeric(String leftPath, String rightLiteral, Map<String, Object> bindings) {
        Object leftObj = resolvePath(leftPath, bindings);
        double left = leftObj instanceof Number n ? n.doubleValue() : Double.parseDouble(leftObj.toString());
        double right = Double.parseDouble(stripQuotes(rightLiteral));
        return Double.compare(left, right);
    }
}
