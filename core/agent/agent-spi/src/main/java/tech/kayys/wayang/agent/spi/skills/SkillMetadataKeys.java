package tech.kayys.wayang.agent.spi.skills;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * Stable metadata keys and typed readers shared by skill providers.
 */
public final class SkillMetadataKeys {

    public static final String KEY_CATEGORY = "category";
    public static final String KEY_DOMAINS = "domains";
    public static final String KEY_INPUT_SCHEMA = "inputSchema";
    public static final String KEY_OUTPUT_FORMAT = "output-format";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_VERSION = "version";

    public static final String DEFAULT_CATEGORY = "custom";
    public static final String DEFAULT_VERSION = "1.0.0";
    public static final String WILDCARD_DOMAIN = "*";

    private SkillMetadataKeys() {
    }

    public static String category(Map<String, ?> metadata) {
        return category(metadata, DEFAULT_CATEGORY);
    }

    public static String category(Map<String, ?> metadata, String fallback) {
        String defaultValue = hasText(fallback) ? fallback.trim() : DEFAULT_CATEGORY;
        return stringValue(metadata, KEY_CATEGORY).orElse(defaultValue);
    }

    public static List<String> domains(Map<String, ?> metadata) {
        return stringList(metadata, KEY_DOMAINS);
    }

    public static Optional<String> outputFormat(Map<String, ?> metadata) {
        return stringValue(metadata, KEY_OUTPUT_FORMAT);
    }

    public static List<String> tags(Map<String, ?> metadata) {
        return stringList(metadata, KEY_TAGS);
    }

    public static String version(Map<String, ?> metadata) {
        return stringValue(metadata, KEY_VERSION).orElse(DEFAULT_VERSION);
    }

    public static List<String> stringList(Map<String, ?> metadata, String key) {
        Objects.requireNonNull(key, "key");
        Object value = value(metadata, key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(SkillMetadataKeys::hasText)
                    .toList();
        }
        if (value instanceof String text) {
            return splitTokens(text);
        }
        return List.of();
    }

    public static Optional<String> stringValue(Map<String, ?> metadata, String key) {
        Objects.requireNonNull(key, "key");
        Object value = value(metadata, key);
        if (value instanceof String text && hasText(text)) {
            return Optional.of(text.trim());
        }
        return Optional.empty();
    }

    private static Object value(Map<String, ?> metadata, String key) {
        return metadata == null ? null : metadata.get(key);
    }

    private static List<String> splitTokens(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .map(String::trim)
                .filter(SkillMetadataKeys::hasText)
                .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
