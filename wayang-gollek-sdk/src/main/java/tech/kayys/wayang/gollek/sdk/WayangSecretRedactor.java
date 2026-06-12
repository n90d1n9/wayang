package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared redaction helper for SDK diagnostics and configuration projections.
 *
 * <p>The helper keeps connection strings usable for routing context while
 * removing common inline credentials from JSON/text diagnostics.</p>
 */
public final class WayangSecretRedactor {

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token|api[-_]?key|access[-_]?key[-_]?id|access[-_]?key|secret[-_]?access[-_]?key)\\s*([:=])\\s*\"?([^\"\\s,;&}\\]]+)\"?");
    private static final Pattern URI_USERINFO_PASSWORD = Pattern.compile(
            "://([^:/?#\\s]+):([^@/?#\\s]+)@");

    private WayangSecretRedactor() {
    }

    public static String connectionString(String value) {
        String trimmed = SdkText.trimToEmpty(value);
        if (trimmed.isBlank()) {
            return "";
        }
        return URI_USERINFO_PASSWORD
                .matcher(SECRET_ASSIGNMENT.matcher(trimmed).replaceAll("$1$2<redacted>"))
                .replaceAll("://$1:<redacted>@");
    }

    public static Object diagnosticValue(Object value) {
        if (value instanceof String text) {
            return connectionString(text);
        }
        if (value instanceof Map<?, ?> map) {
            return diagnosticMap(map);
        }
        if (value instanceof Iterable<?> iterable) {
            return diagnosticList(iterable);
        }
        return value;
    }

    public static Map<String, Object> diagnosticMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(String.valueOf(key));
            if (!normalizedKey.isEmpty() && value != null) {
                copy.put(normalizedKey, diagnosticValue(value));
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    public static List<Object> diagnosticList(Iterable<?> values) {
        if (values == null) {
            return List.of();
        }
        java.util.ArrayList<Object> copy = new java.util.ArrayList<>();
        values.forEach(value -> {
            if (value != null) {
                copy.add(diagnosticValue(value));
            }
        });
        return copy.isEmpty() ? List.of() : Collections.unmodifiableList(copy);
    }
}
