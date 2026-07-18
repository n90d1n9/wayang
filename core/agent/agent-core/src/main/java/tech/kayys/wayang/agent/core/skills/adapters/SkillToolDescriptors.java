package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared descriptor and schema projection for skills exposed through the tool SPI.
 */
final class SkillToolDescriptors {

    static final String KEY_PROPERTIES = "properties";
    static final String KEY_REQUIRED = "required";
    static final String KEY_SKILL_ID = SkillContextKeys.KEY_SKILL_ID;
    static final String KEY_SKILL_TAGS = SkillContextKeys.KEY_SKILL_TAGS;
    static final String KEY_SKILL_VERSION = SkillContextKeys.KEY_SKILL_VERSION;
    static final String KEY_TYPE = "type";
    static final String TYPE_ARRAY = "array";
    static final String TYPE_OBJECT = "object";
    static final String TYPE_STRING = "string";

    private SkillToolDescriptors() {
    }

    static String toolName(SkillDefinition skill, SkillMetadata metadata) {
        Objects.requireNonNull(skill, "skill");
        if (metadata != null && hasText(metadata.name())) {
            return metadata.name().trim();
        }
        return hasText(skill.name()) ? skill.name().trim() : skill.id();
    }

    static String description(SkillMetadata metadata) {
        return metadata == null || metadata.description() == null ? "" : metadata.description();
    }

    static Map<String, Object> inputSchema(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        Object schema = skill.metadata().get(SkillMetadataKeys.KEY_INPUT_SCHEMA);
        if (schema instanceof Map<?, ?> map && !map.isEmpty()) {
            return immutableStringKeyMap(map);
        }
        return defaultInputSchema();
    }

    static SkillMetadata metadataFrom(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        Map<String, String> customMetadata = new LinkedHashMap<>();
        customMetadata.put(SkillMetadataKeys.KEY_VERSION, SkillMetadataKeys.version(skill.metadata()));
        customMetadata.put(
                SkillMetadataKeys.KEY_CATEGORY,
                SkillMetadataKeys.category(skill.metadata(), skill.category()));
        List<String> tags = SkillMetadataKeys.tags(skill.metadata());
        if (!tags.isEmpty()) {
            customMetadata.put(SkillMetadataKeys.KEY_TAGS, String.join(",", tags));
        }
        return SkillMetadata.builder()
                .name(hasText(skill.name()) ? skill.name().trim() : skill.id())
                .description(descriptionFrom(skill))
                .customMetadata(Map.copyOf(customMetadata))
                .build();
    }

    private static String descriptionFrom(SkillDefinition skill) {
        return hasText(skill.description()) ? skill.description().trim() : "Skill " + skill.id();
    }

    private static Map<String, Object> defaultInputSchema() {
        return Map.of(
                KEY_TYPE, TYPE_OBJECT,
                KEY_PROPERTIES, Map.of(
                        KEY_SKILL_ID, Map.of(KEY_TYPE, TYPE_STRING),
                        KEY_SKILL_VERSION, Map.of(KEY_TYPE, TYPE_STRING),
                        KEY_SKILL_TAGS, Map.of(
                                KEY_TYPE, TYPE_ARRAY,
                                "items", Map.of(KEY_TYPE, TYPE_STRING))),
                KEY_REQUIRED, List.of(KEY_SKILL_ID));
    }

    private static Map<String, Object> immutableStringKeyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                copy.put(String.valueOf(key), immutableValue(value));
            }
        });
        return Map.copyOf(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return immutableStringKeyMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(SkillToolDescriptors::immutableValue)
                    .toList();
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
