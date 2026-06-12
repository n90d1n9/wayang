package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses learned-skill persistence hints from Hermes runtime config.
 */
final class HermesPersistenceHintParser {

    private HermesPersistenceHintParser() {
    }

    static Map<String, String> parse(HermesConfigValues values, String propertyPrefix) {
        Map<String, String> hints = new LinkedHashMap<>();
        values.get("persistence-hints", "persistenceHints")
                .map(HermesPersistenceHintParser::mapValue)
                .ifPresent(hints::putAll);
        values.flattened().forEach((key, value) -> persistenceHintName(key, propertyPrefix).ifPresent(name -> {
            if (value != null && !value.isBlank()) {
                hints.put(name, value.trim());
            }
        }));
        return hints;
    }

    private static Optional<String> persistenceHintName(String key, String propertyPrefix) {
        String normalizedPrefix = HermesConfigValues.normalizeKey(propertyPrefix);
        String normalizedKey = HermesConfigValues.normalizeKey(key);
        if (!normalizedKey.startsWith(normalizedPrefix)) {
            return Optional.empty();
        }
        String normalizedRemainder = normalizedKey.substring(normalizedPrefix.length());
        for (String prefix : List.of("persistencehints", "persistencehint")) {
            if (normalizedRemainder.startsWith(prefix) && normalizedRemainder.length() > prefix.length()) {
                return Optional.of(normalizedRemainder.substring(prefix.length()));
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> mapValue(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : value.split("[,;]")) {
            String trimmed = entry.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            int separator = firstSeparator(trimmed);
            if (separator <= 0 || separator >= trimmed.length() - 1) {
                throw new IllegalArgumentException("Invalid Hermes persistence hint value: " + value);
            }
            result.put(HermesConfigValues.normalizeKey(trimmed.substring(0, separator)),
                    trimmed.substring(separator + 1).trim());
        }
        return result;
    }

    private static int firstSeparator(String value) {
        int equals = value.indexOf('=');
        int colon = value.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }
}
