package tech.kayys.wayang.agent.skills.management;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Shared UTF-8 and Java properties helpers for persistence codecs.
 */
final class SkillManagementPropertiesCodecSupport {

    private SkillManagementPropertiesCodecSupport() {
    }

    static byte[] storeToBytes(Properties properties, String comment, String failureMessage) {
        return toUtf8Bytes(storeToString(properties, comment, failureMessage));
    }

    static String storeToString(Properties properties, String comment, String failureMessage) {
        try (StringWriter writer = new StringWriter()) {
            Objects.requireNonNull(properties, "properties").store(writer, comment);
            return writer.toString();
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static Properties loadFromBytes(byte[] content, String failureMessage) {
        return loadFromString(fromUtf8Bytes(content), failureMessage);
    }

    static Properties loadFromString(String content, String failureMessage) {
        Properties properties = new Properties();
        try (Reader reader = new StringReader(Objects.requireNonNull(content, "content"))) {
            properties.load(reader);
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage, error);
        }
        return properties;
    }

    static String requiredProperty(Properties properties, String key, String sourceDescription) {
        String value = Objects.requireNonNull(properties, "properties")
                .getProperty(Objects.requireNonNull(key, "key"));
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required property '" + key + "' in " + sourceDescription);
        }
        return value;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static void putProperty(Properties properties, String key, Object value) {
        if (value != null) {
            Objects.requireNonNull(properties, "properties")
                    .setProperty(Objects.requireNonNull(key, "key"), String.valueOf(value));
        }
    }

    static void putLineTokens(Properties properties, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            putProperty(properties, key, String.join("\n", values));
        }
    }

    static void putPrefixedStringProperties(Properties properties, String prefix, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.entrySet().stream()
                .filter(entry -> !isBlank(entry.getKey()) && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> putProperty(properties, prefix + entry.getKey(), entry.getValue()));
    }

    static void putPrefixedScalarProperties(Properties properties, String prefix, Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.entrySet().stream()
                .filter(entry -> !isBlank(entry.getKey()))
                .filter(entry -> isScalar(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> putProperty(properties, prefix + entry.getKey(), entry.getValue()));
    }

    static Map<String, String> prefixedStringProperties(Properties properties, String prefix) {
        Map<String, String> values = new LinkedHashMap<>();
        Objects.requireNonNull(properties, "properties").stringPropertyNames().stream()
                .filter(key -> key.startsWith(Objects.requireNonNull(prefix, "prefix")))
                .sorted()
                .forEach(key -> values.put(key.substring(prefix.length()), properties.getProperty(key)));
        return Collections.unmodifiableMap(values);
    }

    static List<String> lineTokens(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    static Double doubleOrNull(String value) {
        return isBlank(value) ? null : Double.parseDouble(value);
    }

    static Integer integerOrNull(String value) {
        return isBlank(value) ? null : Integer.parseInt(value);
    }

    static Instant instantOrNull(String value) {
        return isBlank(value) ? null : Instant.parse(value);
    }

    static int integerOrDefault(String value, int defaultValue) {
        return isBlank(value) ? defaultValue : Integer.parseInt(value);
    }

    private static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    static byte[] toUtf8Bytes(String content) {
        return Objects.requireNonNull(content, "content").getBytes(StandardCharsets.UTF_8);
    }

    static String fromUtf8Bytes(byte[] content) {
        return new String(Objects.requireNonNull(content, "content"), StandardCharsets.UTF_8);
    }
}
