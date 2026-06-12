package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Allow/deny policy for dynamic A2UI session config source specifications.
 */
public record SessionConfigSourcePolicy(
        Set<String> allowedTypes,
        Set<String> deniedTypes) {

    private static final SessionConfigSourcePolicy ALLOW_ALL =
            new SessionConfigSourcePolicy(Set.of(), Set.of());

    public SessionConfigSourcePolicy {
        allowedTypes = normalizeTypes(allowedTypes);
        deniedTypes = normalizeTypes(deniedTypes);
    }

    public static SessionConfigSourcePolicy allowAll() {
        return ALLOW_ALL;
    }

    public static SessionConfigSourcePolicy allowOnly(String... sourceTypes) {
        return allowOnly(sourceTypes == null ? List.of() : Arrays.asList(sourceTypes));
    }

    public static SessionConfigSourcePolicy allowOnly(Collection<String> sourceTypes) {
        return new SessionConfigSourcePolicy(asSet(sourceTypes), Set.of());
    }

    public static SessionConfigSourcePolicy deny(String... sourceTypes) {
        return deny(sourceTypes == null ? List.of() : Arrays.asList(sourceTypes));
    }

    public static SessionConfigSourcePolicy deny(Collection<String> sourceTypes) {
        return new SessionConfigSourcePolicy(Set.of(), asSet(sourceTypes));
    }

    public boolean allowAllTypes() {
        return allowedTypes.isEmpty() && deniedTypes.isEmpty();
    }

    public boolean allows(String sourceType) {
        String normalized = canonicalType(sourceType);
        if (normalized.isBlank()) {
            return false;
        }
        if (deniedTypes.contains(normalized)) {
            return false;
        }
        return allowedTypes.isEmpty() || allowedTypes.contains(normalized);
    }

    public List<String> validationErrors(Map<?, ?> rawValues) {
        if (allowAllTypes()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        validateSpec(TransportMaps.copy(rawValues), "source", errors);
        return List.copyOf(errors);
    }

    public void requireAllowed(Map<?, ?> rawValues) {
        List<String> errors = validationErrors(rawValues);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("A2UI session config source policy rejected spec: "
                    + String.join("; ", errors));
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedTypes", List.copyOf(allowedTypes));
        values.put("deniedTypes", List.copyOf(deniedTypes));
        values.put("allowAll", allowAllTypes());
        return TransportMaps.freeze(values);
    }

    private void validateSpec(Map<String, Object> spec, String label, List<String> errors) {
        String type = sourceType(spec);
        String canonical = canonicalType(type);
        if (canonical.isBlank()) {
            errors.add(label + " type must not be blank");
            return;
        }
        if (!allows(canonical)) {
            errors.add(label + " source type " + canonical + " is not allowed");
        }
        if (SessionConfigSourceSpec.TYPE_FALLBACK.equals(canonical)
                || spec.containsKey(SessionConfigSourceRegistry.KEY_SOURCES)) {
            validateSources(spec.get(SessionConfigSourceRegistry.KEY_SOURCES), label, errors);
        }
    }

    private void validateSources(Object rawSources, String label, List<String> errors) {
        if (rawSources instanceof List<?> sources) {
            for (int index = 0; index < sources.size(); index++) {
                Object source = sources.get(index);
                if (source instanceof Map<?, ?> sourceMap) {
                    validateSpec(TransportMaps.copy(sourceMap), label + ".sources[" + index + "]", errors);
                }
            }
            return;
        }
        if (rawSources instanceof Map<?, ?> sourceMap) {
            validateSpec(TransportMaps.copy(sourceMap), label + ".sources[0]", errors);
        }
    }

    private static String sourceType(Map<String, Object> values) {
        String type = text(values.get(SessionConfigSourceRegistry.KEY_TYPE));
        if (type.isBlank()) {
            type = text(values.get(SessionConfigSourceRegistry.KEY_KIND));
        }
        if (type.isBlank()) {
            type = inferredType(values);
        }
        return type;
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

    private static Set<String> normalizeTypes(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.stream()
                .map(SessionConfigSourcePolicy::canonicalType)
                .filter(value -> !value.isBlank())
                .forEach(normalized::add);
        return Collections.unmodifiableSet(normalized);
    }

    private static Set<String> asSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(values);
    }

    private static String canonicalType(String sourceType) {
        String normalized = normalizeType(sourceType);
        return switch (normalized) {
            case "json" -> SessionConfigSourceSpec.TYPE_INLINE;
            case "resource" -> SessionConfigSourceSpec.TYPE_CLASSPATH;
            case "chain", "first-available" -> SessionConfigSourceSpec.TYPE_FALLBACK;
            default -> normalized;
        };
    }

    private static String normalizeType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "";
        }
        return sourceType.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
