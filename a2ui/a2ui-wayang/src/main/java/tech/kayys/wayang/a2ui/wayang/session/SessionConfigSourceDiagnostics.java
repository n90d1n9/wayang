package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Operator-facing diagnostic payload for one A2UI session config source specification.
 */
public record SessionConfigSourceDiagnostics(
        String diagnosticsId,
        String sourceType,
        Map<String, Object> sourceSpec,
        SessionConfigLoadResult loadResult,
        Map<String, Object> sourcePolicy,
        List<String> validationErrors,
        List<String> policyErrors,
        Map<String, Object> capability,
        Map<String, Object> sourceCapabilities) {

    public static final String DIAGNOSTICS_ID = "a2ui.session-config.source-diagnostics";

    public SessionConfigSourceDiagnostics {
        diagnosticsId = DecodeValues.text(diagnosticsId, DIAGNOSTICS_ID);
        sourceType = DecodeValues.text(sourceType, "unknown");
        sourceSpec = TransportMaps.copy(sourceSpec);
        loadResult = loadResult == null ? SessionConfigLoadResult.missing("session-config-source") : loadResult;
        sourcePolicy = TransportMaps.copy(sourcePolicy);
        validationErrors = copyMessages(validationErrors);
        policyErrors = copyMessages(policyErrors);
        capability = TransportMaps.copy(capability);
        sourceCapabilities = TransportMaps.copy(sourceCapabilities);
    }

    public static SessionConfigSourceDiagnostics load(
            SessionConfigSourceSpec spec,
            SessionConfigSourceRegistry registry) {
        return load(Objects.requireNonNull(spec, "spec").toMap(), registry);
    }

    public static SessionConfigSourceDiagnostics load(
            Map<?, ?> rawSpec,
            SessionConfigSourceRegistry registry) {
        SessionConfigSourceRegistry resolvedRegistry = registry == null
                ? SessionConfigSourceRegistry.standard()
                : registry;
        Map<String, Object> values = TransportMaps.copy(rawSpec);
        String type = sourceType(values);
        List<String> validationErrors = SessionConfigSourceSpec.validationErrors(values);
        List<String> policyErrors = validationErrors.isEmpty()
                ? resolvedRegistry.sourcePolicy().validationErrors(values)
                : List.of();
        SessionConfigLoadResult result = loadResult(values, resolvedRegistry, type, validationErrors, policyErrors);
        return new SessionConfigSourceDiagnostics(
                DIAGNOSTICS_ID,
                type.isBlank() ? "unknown" : type,
                SessionConfigSourceSpec.redactedMap(values),
                result,
                resolvedRegistry.sourcePolicy().toMap(),
                validationErrors,
                policyErrors,
                capability(type, resolvedRegistry),
                sourceCapabilities(resolvedRegistry));
    }

    public static SessionConfigSourceDiagnostics failed(
            String sourceDescription,
            RuntimeException failure,
            SessionConfigSourceRegistry registry) {
        SessionConfigSourceRegistry resolvedRegistry = registry == null
                ? SessionConfigSourceRegistry.standard()
                : registry;
        String message = failure == null ? "" : DecodeValues.text(failure.getMessage());
        return new SessionConfigSourceDiagnostics(
                DIAGNOSTICS_ID,
                "unknown",
                Map.of(),
                SessionConfigLoadResult.failed(
                        DecodeValues.text(sourceDescription, "session-config-source"),
                        failure),
                resolvedRegistry.sourcePolicy().toMap(),
                message.isBlank() ? List.of() : List.of(message),
                List.of(),
                Map.of(),
                sourceCapabilities(resolvedRegistry));
    }

    public static SessionConfigSourceDiagnostics fromMap(Map<?, ?> values) {
        return SessionConfigSourceDiagnosticsDecoder.fromMap(values);
    }

    public static SessionConfigSourceDiagnostics fromJson(String json) {
        return SessionConfigSourceDiagnosticsDecoder.fromJson(json);
    }

    public boolean valid() {
        return validationErrors.isEmpty();
    }

    public boolean allowed() {
        return policyErrors.isEmpty();
    }

    public SessionConfigLoadStatus status() {
        return loadResult.status();
    }

    public boolean loaded() {
        return loadResult.loaded();
    }

    public boolean missing() {
        return loadResult.missing();
    }

    public boolean failed() {
        return loadResult.failed();
    }

    public Map<String, Object> toMap() {
        return SessionProjection.sourceDiagnostics(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI session config source diagnostics");
    }

    private static SessionConfigLoadResult loadResult(
            Map<String, Object> values,
            SessionConfigSourceRegistry registry,
            String type,
            List<String> validationErrors,
            List<String> policyErrors) {
        if (!validationErrors.isEmpty()) {
            return SessionConfigLoadResult.failed(
                    "session-config-source-spec",
                    new IllegalArgumentException(String.join("; ", validationErrors)));
        }
        if (!policyErrors.isEmpty()) {
            return SessionConfigLoadResult.failed(
                    "session-config-source-policy",
                    new IllegalArgumentException(String.join("; ", policyErrors)));
        }
        try {
            return registry.source(values).loadResult();
        } catch (RuntimeException e) {
            return SessionConfigLoadResult.failed("session-config-source:" + type, e);
        }
    }

    private static Map<String, Object> capability(
            String type,
            SessionConfigSourceRegistry registry) {
        return registry.capability(type)
                .map(SessionConfigSourceCapability::toMap)
                .orElseGet(Map::of);
    }

    private static Map<String, Object> sourceCapabilities(SessionConfigSourceRegistry registry) {
        Map<String, Object> values = new LinkedHashMap<>();
        registry.sourceCapabilities()
                .forEach((type, capability) -> values.put(type, capability.toMap()));
        return TransportMaps.freeze(values);
    }

    private static String sourceType(Map<String, Object> values) {
        String type = DecodeValues.text(values.get(SessionConfigSourceRegistry.KEY_TYPE));
        if (type.isBlank()) {
            type = DecodeValues.text(values.get(SessionConfigSourceRegistry.KEY_KIND));
        }
        if (type.isBlank()) {
            type = inferredType(values);
        }
        return canonicalType(type);
    }

    private static String inferredType(Map<String, Object> values) {
        if (values.containsKey(SessionConfigSourceRegistry.KEY_SOURCES)) {
            return SessionConfigSourceSpec.TYPE_FALLBACK;
        }
        if (values.containsKey("path") || values.containsKey("file")) {
            return SessionConfigSourceSpec.TYPE_FILE;
        }
        if (values.containsKey("resource") || values.containsKey("classpath")) {
            return SessionConfigSourceSpec.TYPE_CLASSPATH;
        }
        if (values.containsKey("json") || values.containsKey("value")) {
            return SessionConfigSourceSpec.TYPE_INLINE;
        }
        return "";
    }

    private static String canonicalType(String type) {
        String normalized = type == null || type.isBlank()
                ? ""
                : type.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "json" -> SessionConfigSourceSpec.TYPE_INLINE;
            case "resource" -> SessionConfigSourceSpec.TYPE_CLASSPATH;
            case "chain", "first-available" -> SessionConfigSourceSpec.TYPE_FALLBACK;
            default -> normalized;
        };
    }

    private static List<String> copyMessages(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(DecodeValues::text)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
