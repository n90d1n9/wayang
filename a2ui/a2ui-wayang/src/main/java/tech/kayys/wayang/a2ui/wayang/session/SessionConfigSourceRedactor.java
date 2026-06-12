package tech.kayys.wayang.a2ui.wayang.session;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Diagnostic redaction policy for A2UI session config source specifications.
 */
public final class SessionConfigSourceRedactor {

    public static final String REDACTION_MARKER = "[REDACTED]";

    private static final Set<String> SENSITIVE_EXACT_KEYS = Set.of(
            "auth",
            "authorization",
            "bearer",
            "connectionstring",
            "credential",
            "credentials",
            "dsn",
            "endpoint",
            "endpointurl",
            "passwd",
            "password",
            "pwd",
            "secret",
            "token",
            "uri",
            "url");
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "accesskey",
            "apikey",
            "authorization",
            "clientsecret",
            "connectionstring",
            "credential",
            "password",
            "privatekey",
            "secret",
            "token");

    public static Map<String, Object> redact(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> redacted = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                String textKey = String.valueOf(key);
                redacted.put(textKey, redactValue(textKey, value));
            }
        });
        return freeze(redacted);
    }

    public static boolean sensitiveKey(String key) {
        String normalized = normalizeKey(key);
        if (normalized.isBlank()) {
            return false;
        }
        if (SENSITIVE_EXACT_KEYS.contains(normalized)
                || normalized.contains("endpoint")
                || normalized.endsWith("uri")
                || normalized.endsWith("url")) {
            return true;
        }
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    private static Object redactValue(String key, Object value) {
        if (sensitiveKey(key)) {
            return REDACTION_MARKER;
        }
        if (value instanceof Map<?, ?> map) {
            return redact(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(entry -> entry != null)
                    .map(SessionConfigSourceRedactor::redactListValue)
                    .toList();
        }
        return value;
    }

    private static Object redactListValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return redact(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(entry -> entry != null)
                    .map(SessionConfigSourceRedactor::redactListValue)
                    .toList();
        }
        return value;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        key.toLowerCase(Locale.ROOT).chars()
                .filter(Character::isLetterOrDigit)
                .forEach(character -> normalized.append((char) character));
        return normalized.toString();
    }

    private static Map<String, Object> freeze(Map<String, Object> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private SessionConfigSourceRedactor() {
    }
}
