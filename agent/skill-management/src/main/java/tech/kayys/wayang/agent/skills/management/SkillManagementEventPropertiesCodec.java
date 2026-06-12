package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

/**
 * Properties encoding shared by filesystem and JDBC event history stores.
 */
final class SkillManagementEventPropertiesCodec {

    private static final String ATTRIBUTE_PREFIX = "attribute.";
    private static final String KEY_OCCURRED_AT = "occurredAt";
    private static final String KEY_OPERATION = "operation";
    private static final String KEY_SKILL_ID = "skillId";
    private static final String KEY_SUCCESS = "success";

    private SkillManagementEventPropertiesCodec() {
    }

    static Properties toProperties(SkillManagementEvent event) {
        Properties properties = new Properties();
        properties.setProperty(KEY_OCCURRED_AT, event.occurredAt().toString());
        properties.setProperty(KEY_OPERATION, event.operation().name());
        properties.setProperty(KEY_SKILL_ID, event.skillId());
        properties.setProperty(KEY_SUCCESS, Boolean.toString(event.success()));
        event.attributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> properties.setProperty(ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue()));
        return properties;
    }

    static SkillManagementEvent fromProperties(Properties properties, String sourceDescription) {
        return new SkillManagementEvent(
                Instant.parse(SkillManagementPropertiesCodecSupport.requiredProperty(
                        properties,
                        KEY_OCCURRED_AT,
                        sourceDescription)),
                SkillManagementEventOperation.valueOf(SkillManagementPropertiesCodecSupport.requiredProperty(
                        properties,
                        KEY_OPERATION,
                        sourceDescription)),
                properties.getProperty(KEY_SKILL_ID, ""),
                Boolean.parseBoolean(SkillManagementPropertiesCodecSupport.requiredProperty(
                        properties,
                        KEY_SUCCESS,
                        sourceDescription)),
                attributes(properties));
    }

    static byte[] toBytes(SkillManagementEvent event) {
        return SkillManagementPropertiesCodecSupport.storeToBytes(
                toProperties(event),
                "Wayang skill-management event",
                "Failed to encode skill-management event");
    }

    static SkillManagementEvent fromBytes(byte[] content, String sourceDescription) {
        Properties properties = SkillManagementPropertiesCodecSupport.loadFromBytes(
                content,
                "Failed to decode skill-management event from " + sourceDescription);
        return fromProperties(properties, sourceDescription);
    }

    static String encodeAttributes(Map<String, String> attributes) {
        Properties properties = new Properties();
        if (attributes != null) {
            attributes.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> properties.setProperty(ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue()));
        }
        return SkillManagementPropertiesCodecSupport.storeToString(
                properties,
                "Wayang skill-management event attributes",
                "Failed to encode skill-management event attributes");
    }

    static Map<String, String> decodeAttributes(String content, String sourceDescription) {
        if (SkillManagementPropertiesCodecSupport.isBlank(content)) {
            return Map.of();
        }
        Properties properties = SkillManagementPropertiesCodecSupport.loadFromString(
                content,
                "Failed to decode skill-management event attributes from " + sourceDescription);
        return attributes(properties);
    }

    private static Map<String, String> attributes(Properties properties) {
        return SkillManagementPropertiesCodecSupport.prefixedStringProperties(properties, ATTRIBUTE_PREFIX);
    }
}
