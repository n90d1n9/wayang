package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Shared map/property/environment normalization helpers for skill store config.
 */
final class SkillStoreConfigValues {

    private SkillStoreConfigValues() {
    }

    static Map<String, Object> fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        Map<String, Object> values = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(key -> values.put(key, properties.getProperty(key)));
        return values;
    }

    static Map<String, Object> fromEnvironment(
            Map<String, String> environment,
            String environmentPrefix,
            String propertyPrefix) {
        Map<String, Object> values = new LinkedHashMap<>();
        Objects.requireNonNull(environment, "environment").forEach((key, value) -> {
            values.put(key, value);
            if (key.startsWith(environmentPrefix)) {
                String suffix = key.substring(environmentPrefix.length())
                        .toLowerCase(Locale.ROOT)
                        .replace('_', '.');
                values.put(propertyPrefix + suffix, value);
            }
        });
        return values;
    }

    static Map<String, String> flattenAndNormalize(Map<String, ?> values) {
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", Objects.requireNonNull(values, "values"), flattened);
        Map<String, String> normalized = new LinkedHashMap<>();
        flattened.forEach((key, value) -> normalized.put(normalize(key), value));
        return normalized;
    }

    static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        value.toLowerCase(Locale.ROOT).chars()
                .filter(Character::isLetterOrDigit)
                .forEach(codePoint -> builder.append((char) codePoint));
        return builder.toString();
    }

    static boolean booleanValue(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "true", "yes", "y", "1", "on", "enabled" -> true;
            case "false", "no", "n", "0", "off", "disabled" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean skill store config value: " + value);
        };
    }

    static String required(ScopedValues scoped, String message, String... keys) {
        return scoped.get(keys)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, ?> values, Map<String, String> output) {
        values.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String path = prefix.isBlank() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                flatten(path, (Map<String, ?>) nested, output);
            } else {
                output.put(path, String.valueOf(value));
            }
        });
    }

    record ScopedValues(Map<String, String> values, String prefix) {

        Optional<String> get(String... keys) {
            for (String key : keys) {
                String value = values.get(normalize(prefix + key));
                if (value != null) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }

        boolean hasChild(String childName) {
            String childPrefix = normalize(prefix + childName);
            return values.keySet().stream().anyMatch(key -> key.startsWith(childPrefix));
        }
    }
}
