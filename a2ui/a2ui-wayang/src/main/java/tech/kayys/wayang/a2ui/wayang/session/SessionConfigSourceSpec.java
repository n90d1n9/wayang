package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Structured, storage-neutral specification for locating A2UI session configuration JSON.
 */
public record SessionConfigSourceSpec(Map<String, Object> values) {

    public static final String TYPE_INLINE = "inline";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_CLASSPATH = "classpath";
    public static final String TYPE_FALLBACK = "fallback";

    private static final Set<String> INLINE_TYPES = Set.of(TYPE_INLINE, "json");
    private static final Set<String> FILE_TYPES = Set.of(TYPE_FILE);
    private static final Set<String> CLASSPATH_TYPES = Set.of(TYPE_CLASSPATH, "resource");
    private static final Set<String> FALLBACK_TYPES = Set.of("chain", TYPE_FALLBACK, "first-available");

    public SessionConfigSourceSpec {
        values = TransportMaps.copy(values);
        requireValid(values);
    }

    public static SessionConfigSourceSpec inlineJson(String json) {
        return inlineJson("inline", json);
    }

    public static SessionConfigSourceSpec inlineJson(String description, String json) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigSourceRegistry.KEY_TYPE, TYPE_INLINE);
        values.put("description", description);
        values.put("json", json);
        return new SessionConfigSourceSpec(values);
    }

    public static SessionConfigSourceSpec file(Path path) {
        Path resolved = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigSourceRegistry.KEY_TYPE, TYPE_FILE);
        values.put("path", resolved.toString());
        return new SessionConfigSourceSpec(values);
    }

    public static SessionConfigSourceSpec classpath(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("resourceName must not be blank");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigSourceRegistry.KEY_TYPE, TYPE_CLASSPATH);
        values.put("resource", resourceName);
        return new SessionConfigSourceSpec(values);
    }

    public static SessionConfigSourceSpec provider(String type, Map<?, ?> values) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        Map<String, Object> spec = new LinkedHashMap<>(TransportMaps.copy(values));
        spec.put(SessionConfigSourceRegistry.KEY_TYPE, type.trim());
        return new SessionConfigSourceSpec(spec);
    }

    public static SessionConfigSourceSpec firstAvailable(SessionConfigSourceSpec... sources) {
        List<Map<String, Object>> specs = new ArrayList<>();
        if (sources != null) {
            for (SessionConfigSourceSpec source : sources) {
                if (source != null) {
                    specs.add(source.toMap());
                }
            }
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigSourceRegistry.KEY_TYPE, TYPE_FALLBACK);
        values.put(SessionConfigSourceRegistry.KEY_SOURCES, specs);
        return new SessionConfigSourceSpec(values);
    }

    public Map<String, Object> toMap() {
        return values;
    }

    public Map<String, Object> toDiagnosticMap() {
        return redactedMap(values);
    }

    public List<String> validationErrors() {
        return validationErrors(values);
    }

    public SessionConfigSource source() {
        return source(SessionConfigSourceRegistry.standard());
    }

    public SessionConfigSource source(SessionConfigSourceRegistry registry) {
        SessionConfigSourceRegistry resolved = registry == null ? SessionConfigSourceRegistry.standard() : registry;
        return resolved.source(this);
    }

    public static List<String> validationErrors(Map<?, ?> rawValues) {
        Map<String, Object> spec = TransportMaps.copy(rawValues);
        List<String> errors = new ArrayList<>();
        validateSpec(spec, "source", errors);
        return List.copyOf(errors);
    }

    public static Map<String, Object> redactedMap(Map<?, ?> rawValues) {
        return SessionConfigSourceRedactor.redact(rawValues);
    }

    public static void requireValid(Map<?, ?> rawValues) {
        List<String> errors = validationErrors(rawValues);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid A2UI session config source spec: "
                    + String.join("; ", errors));
        }
    }

    private static void validateSpec(Map<String, Object> spec, String label, List<String> errors) {
        String type = sourceType(spec);
        if (type.isBlank()) {
            errors.add(label + " type must not be blank");
            return;
        }
        if (INLINE_TYPES.contains(type) && !hasText(spec, "json", "value")) {
            errors.add(label + " inline source requires json");
        }
        if (FILE_TYPES.contains(type) && !hasText(spec, "path", "file")) {
            errors.add(label + " file source requires path");
        }
        if (CLASSPATH_TYPES.contains(type) && !hasText(spec, "resource", "classpath")) {
            errors.add(label + " classpath source requires resource");
        }
        if (FALLBACK_TYPES.contains(type) || spec.containsKey(SessionConfigSourceRegistry.KEY_SOURCES)) {
            validateSources(spec.get(SessionConfigSourceRegistry.KEY_SOURCES), label, errors);
        }
    }

    private static void validateSources(Object rawSources, String label, List<String> errors) {
        if (rawSources instanceof List<?> sources) {
            if (sources.isEmpty()) {
                errors.add(label + " fallback source requires at least one nested source");
                return;
            }
            int validSourceCount = 0;
            for (int index = 0; index < sources.size(); index++) {
                Object entry = sources.get(index);
                if (entry instanceof Map<?, ?> source) {
                    validSourceCount++;
                    validateSpec(TransportMaps.copy(source), label + ".sources[" + index + "]", errors);
                } else {
                    errors.add(label + ".sources[" + index + "] must be a source object");
                }
            }
            if (validSourceCount == 0) {
                errors.add(label + " fallback source requires at least one source object");
            }
            return;
        }
        if (rawSources instanceof Map<?, ?> source) {
            validateSpec(TransportMaps.copy(source), label + ".sources[0]", errors);
            return;
        }
        errors.add(label + " fallback source requires sources");
    }

    private static String sourceType(Map<String, Object> spec) {
        String type = text(spec.get(SessionConfigSourceRegistry.KEY_TYPE));
        if (type.isBlank()) {
            type = text(spec.get(SessionConfigSourceRegistry.KEY_KIND));
        }
        if (type.isBlank()) {
            type = inferredType(spec);
        }
        return normalizeType(type);
    }

    private static String inferredType(Map<String, Object> spec) {
        if (spec.containsKey(SessionConfigSourceRegistry.KEY_SOURCES)) {
            return TYPE_FALLBACK;
        }
        if (spec.containsKey("path") || spec.containsKey("file")) {
            return TYPE_FILE;
        }
        if (spec.containsKey("resource") || spec.containsKey("classpath")) {
            return TYPE_CLASSPATH;
        }
        if (spec.containsKey("json") || spec.containsKey("value")) {
            return TYPE_INLINE;
        }
        return "";
    }

    private static boolean hasText(Map<String, Object> spec, String... keys) {
        for (String key : keys) {
            if (!text(spec.get(key)).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeType(String type) {
        return type.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
    }
}
