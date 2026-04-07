package tech.kayys.wayang.prompt.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PromptRenderer — stateless {{placeholder}} interpolation engine.
 * ============================================================================
 *
 * Resolves every {@code {{variable}}} in a {@link PromptTemplate}'s body
 * according to a strict priority chain:
 *
 * 1. EXPLICIT values – supplied directly by the AgentNode at call time.
 * 2. CONTEXT values – injected by the workflow engine (RAG results,
 * memory snapshots, workflow state).
 * 3. DEFAULT values – declared in the {@link VariableDescriptor}.
 * 4. EMPTY string – allowed only when the variable is optional
 * ({@code required == false}).
 *
 * If a *required* variable reaches step 4 without a value, a
 * {@link PromptRenderException} is thrown. The exception carries enough
 * structure to be mapped directly into an {@code ErrorPayload}.
 *
 * Sensitive-value handling:
 * • The renderer produces *two* copies of the output:
 * – {@code content} – the live string sent to the LLM.
 * – {@code redactedContent} – the same string with every value marked
 * {@code sensitive == true} replaced by
 * {@code ***REDACTED***}.
 * • This lets the platform log the redacted copy to the audit/provenance
 * store without leaking secrets.
 *
 * Type coercion:
 * • Each resolved value is coerced through {@link VariableType#coerce()}
 * before interpolation. An optional {@link JsonCoercer}
 * can be supplied for OBJECT / ARRAY serialisation (platform wires
 * Jackson; standalone uses {@code toString()}).
 *
 * Thread safety:
 * This class is stateless and fully thread-safe. A single instance can
 * be shared across the entire platform.
 */
public final class PromptRenderer {

    /** Sentinel injected for sensitive variables in the redacted copy. */
    public static final String REDACTED_MARKER = "***REDACTED***";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");

    /** Optional Jackson-backed coercer; null means toString() fallback. */
    private final JsonCoercer jsonCoercer;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------
    /** Creates a renderer without a JsonCoercer (standalone-safe). */
    public PromptRenderer() {
        this(null);
    }

    /**
     * Creates a renderer with an explicit JsonCoercer.
     * Platform wiring passes a Jackson-backed instance here.
     */
    public PromptRenderer(JsonCoercer jsonCoercer) {
        this.jsonCoercer = jsonCoercer;
    }

    // ------------------------------------------------------------------
    // Core render method
    // ------------------------------------------------------------------
    /**
     * Renders {@code template} by resolving all placeholders.
     *
     * @param template       the template to render
     * @param explicitValues values supplied by the calling node
     * @param contextValues  values supplied by the workflow context
     * @return an immutable {@link RenderResult}
     * @throws PromptRenderException when a required variable cannot be resolved
     */
    public RenderResult render(
            PromptTemplate template,
            Map<String, Object> explicitValues,
            Map<String, Object> contextValues) throws PromptRenderException {

        Objects.requireNonNull(template, "template must not be null");
        Map<String, Object> explicit = explicitValues != null ? explicitValues : Map.of();
        Map<String, Object> context = contextValues != null ? contextValues : Map.of();

        // Build lookup index: variableName → descriptor
        Map<String, VariableDescriptor> varIndex = template.getVariables().stream()
                .collect(Collectors.toMap(VariableDescriptor::name, v -> v));

        // ── Pass 1: resolve every placeholder, track source ──────────
        Map<String, Object> resolvedValues = new LinkedHashMap<>();
        Map<String, RenderResult.ResolutionSource> resolutionSources = new LinkedHashMap<>();

        for (String placeholder : template.getPlaceholders()) {
            VariableDescriptor desc = varIndex.get(placeholder);
            // If there is no descriptor, the variable is undeclared — still resolve
            // from explicit/context if available, otherwise empty.
            boolean required = desc != null && desc.required();
            boolean sensitive = desc != null && desc.sensitive();
            VariableType type = desc != null ? desc.type() : VariableType.STRING;

            Object value = null;
            RenderResult.ResolutionSource source;

            if (explicit.containsKey(placeholder)) {
                value = explicit.get(placeholder);
                source = RenderResult.ResolutionSource.EXPLICIT;
            } else if (context.containsKey(placeholder)) {
                value = context.get(placeholder);
                source = RenderResult.ResolutionSource.CONTEXT;
            } else if (desc != null && desc.hasDefault()) {
                value = desc.defaultValue();
                source = RenderResult.ResolutionSource.DEFAULT;
            } else if (required) {
                // Required variable with no value anywhere → hard failure
                throw new PromptRenderException(
                        placeholder,
                        template.getTemplateId(),
                        template.getActiveVersion(),
                        "Required variable '%s' could not be resolved in template '%s' v%s"
                                .formatted(placeholder, template.getTemplateId(), template.getActiveVersion()));
            } else {
                value = ""; // optional, no value found
                source = RenderResult.ResolutionSource.EMPTY;
            }

            // Coerce to declared type
            value = type.coerce(value, jsonCoercer);

            resolvedValues.put(placeholder, value);
            resolutionSources.put(placeholder, source);
        }

        // ── Pass 2: interpolate into body (live copy) ────────────────
        String liveBody = interpolate(template.getBody(), resolvedValues);

        // ── Pass 3: interpolate into body (redacted copy) ────────────
        Map<String, Object> redactedMap = new LinkedHashMap<>(resolvedValues);
        for (String placeholder : template.getPlaceholders()) {
            VariableDescriptor desc = varIndex.get(placeholder);
            if (desc != null && desc.sensitive()) {
                redactedMap.put(placeholder, REDACTED_MARKER);
            }
        }
        String redactedBody = interpolate(template.getBody(), redactedMap);

        return new RenderResult(liveBody, redactedBody, resolutionSources, resolvedValues);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------
    /**
     * Substitutes every {@code {{name}}} in {@code template} with the
     * corresponding value from {@code values}. Unknown placeholders are
     * left untouched (defensive).
     */
    private static String interpolate(String template, Map<String, Object> values) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            Object val = values.get(name);
            // If somehow not in the map (shouldn't happen after Pass 1), keep original
            String replacement = val != null ? val.toString() : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
