package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Flattened, normalized view of Hermes runtime config values.
 */
final class HermesConfigValues {

    private final Map<String, String> flattened;
    private final Map<String, String> normalized;
    private final String prefix;

    private HermesConfigValues(Map<String, String> flattened, String prefix) {
        this.flattened = Collections.unmodifiableMap(new LinkedHashMap<>(flattened));
        this.normalized = Collections.unmodifiableMap(normalizeKeys(flattened));
        this.prefix = prefix == null ? "" : prefix;
    }

    static HermesConfigValues from(Map<String, ?> values, String prefix) {
        return new HermesConfigValues(flattenAndStringify(values == null ? Map.of() : values), prefix);
    }

    Map<String, String> flattened() {
        return flattened;
    }

    Optional<String> get(String... keys) {
        for (String key : keys) {
            String value = normalized.get(normalizeKey(prefix + key));
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    Optional<Boolean> booleanValue(String... keys) {
        return get(keys).map(HermesConfigValues::booleanValue);
    }

    Optional<Integer> intValue(String... keys) {
        return get(keys).map(value -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Invalid Hermes integer config value: " + value, error);
            }
        });
    }

    Optional<List<String>> listValue(String... keys) {
        return get(keys).map(HermesConfigValues::listValue);
    }

    static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        value.toLowerCase(Locale.ROOT).chars()
                .filter(Character::isLetterOrDigit)
                .forEach(codePoint -> builder.append((char) codePoint));
        return builder.toString();
    }

    private static boolean booleanValue(String value) {
        return switch (normalizeKey(value)) {
            case "true", "yes", "y", "1", "on", "enabled" -> true;
            case "false", "no", "n", "0", "off", "disabled" -> false;
            default -> throw new IllegalArgumentException("Invalid Hermes boolean config value: " + value);
        };
    }

    private static List<String> listValue(String value) {
        List<String> result = new ArrayList<>();
        for (String entry : value.split("[,;]")) {
            String trimmed = entry.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static Map<String, String> flattenAndStringify(Map<?, ?> values) {
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", values, flattened);
        return flattened;
    }

    private static void flatten(String prefix, Map<?, ?> values, Map<String, String> output) {
        values.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String path = prefix.isBlank() ? String.valueOf(key) : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                flatten(path, nested, output);
            } else {
                output.put(path, String.valueOf(value));
            }
        });
    }

    private static Map<String, String> normalizeKeys(Map<String, String> values) {
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> normalized.put(normalizeKey(key), value));
        return normalized;
    }
}
