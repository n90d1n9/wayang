package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normalized request metadata reader shared by Hermes request planners.
 */
final class HermesRequestValues {

    private final Map<String, Object> values;
    private final String prompt;

    private HermesRequestValues(AgentRequest request) {
        Map<String, Object> collected = new LinkedHashMap<>();
        if (request != null) {
            collected.putAll(request.context());
            collected.putAll(request.parameters());
        }
        this.values = Collections.unmodifiableMap(collected);
        this.prompt = request == null ? "" : HermesText.oneLine(request.prompt());
    }

    static HermesRequestValues from(AgentRequest request) {
        return new HermesRequestValues(request);
    }

    String prompt() {
        return prompt;
    }

    Optional<String> firstText(List<String> keys) {
        for (String key : keys) {
            Object value = lookup(values, key);
            if (value != null && !value.toString().isBlank()) {
                return Optional.of(value.toString().trim());
            }
        }
        return Optional.empty();
    }

    Optional<Boolean> firstBoolean(List<String> keys, String label) {
        for (String key : keys) {
            Object value = lookup(values, key);
            if (value instanceof Boolean bool) {
                return Optional.of(bool);
            }
            if (value != null && !value.toString().isBlank()) {
                return Optional.of(booleanValue(value.toString(), label));
            }
        }
        return Optional.empty();
    }

    Optional<Integer> firstInt(List<String> keys, String label) {
        for (String key : keys) {
            Object value = lookup(values, key);
            if (value instanceof Number number) {
                return Optional.of(number.intValue());
            }
            if (value != null && !value.toString().isBlank()) {
                try {
                    return Optional.of(Integer.parseInt(value.toString().trim()));
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException(
                            "Invalid Hermes " + label + " integer value: " + value,
                            error);
                }
            }
        }
        return Optional.empty();
    }

    List<String> firstList(List<String> keys) {
        for (String key : keys) {
            Object value = lookup(values, key);
            List<String> result = listValue(value);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return List.of();
    }

    static boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .replace(" ", "")
                .trim();
    }

    static String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static Object lookup(Map<String, Object> values, String key) {
        String normalized = normalize(key);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean booleanValue(String value, String label) {
        return switch (normalize(value)) {
            case "true", "yes", "y", "1", "on", "enabled", "required" -> true;
            case "false", "no", "n", "0", "off", "disabled", "none" -> false;
            default -> throw new IllegalArgumentException(
                    "Invalid Hermes " + label + " boolean value: " + value);
        };
    }

    private static List<String> listValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> result = new ArrayList<>();
            for (Object entry : iterable) {
                if (entry != null && !entry.toString().isBlank()) {
                    result.add(entry.toString().trim());
                }
            }
            return List.copyOf(result);
        }
        List<String> result = new ArrayList<>();
        for (String entry : value.toString().split("[,;]")) {
            if (!entry.isBlank()) {
                result.add(entry.trim());
            }
        }
        return List.copyOf(result);
    }
}
