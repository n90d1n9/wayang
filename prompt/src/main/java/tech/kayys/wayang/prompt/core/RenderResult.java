package tech.kayys.wayang.prompt.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================================
 * RenderResult — immutable output of a single template render pass.
 * ============================================================================
 *
 * Carries three views of the rendered content:
 * • {@link #content()} – the fully interpolated string (live value).
 * • {@link #redactedContent()} – same string with sensitive values replaced
 * by {@code ***REDACTED***}. Safe for audit logs.
 * • {@link #resolutionSources()} – per-variable map that records *where* each
 * value came from: EXPLICIT, CONTEXT, DEFAULT,
 * or EMPTY. Useful for provenance and debugging.
 *
 * Also exposes {@link #resolvedValues()} — the final map of variable→value
 * that was actually substituted (after coercion, before string interpolation).
 */
public record RenderResult(
        String content,
        String redactedContent,
        Map<String, ResolutionSource> resolutionSources,
        Map<String, Object> resolvedValues) {
    public RenderResult {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(redactedContent, "redactedContent must not be null");
        resolutionSources = resolutionSources != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(resolutionSources))
                : Collections.emptyMap();
        resolvedValues = resolvedValues != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(resolvedValues))
                : Collections.emptyMap();
    }

    /**
     * Where a variable's value was sourced from during rendering.
     *
     * Priority chain (highest to lowest):
     * EXPLICIT → CONTEXT → DEFAULT → EMPTY
     */
    public enum ResolutionSource {
        /** Supplied explicitly by the calling node (highest priority). */
        EXPLICIT,
        /** Retrieved from the workflow context (RAG, memory, state). */
        CONTEXT,
        /** Fell back to the VariableDescriptor's defaultValue. */
        DEFAULT,
        /**
         * Variable is optional and no value was found; substituted with empty string.
         */
        EMPTY
    }
}