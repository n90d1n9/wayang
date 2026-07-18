package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillToolDescriptorsContractTest {

    @Test
    void exposesStableDefaultToolInputSchema() {
        Map<String, Object> schema = SkillToolDescriptors.inputSchema(skill(Map.of()));

        assertEquals(SkillToolDescriptors.TYPE_OBJECT, schema.get(SkillToolDescriptors.KEY_TYPE));
        assertEquals(List.of(SkillToolDescriptors.KEY_SKILL_ID), schema.get(SkillToolDescriptors.KEY_REQUIRED));
        Map<?, ?> properties = assertInstanceOf(Map.class, schema.get(SkillToolDescriptors.KEY_PROPERTIES));
        Map<?, ?> skillId = assertInstanceOf(Map.class, properties.get(SkillToolDescriptors.KEY_SKILL_ID));
        Map<?, ?> skillTags = assertInstanceOf(Map.class, properties.get(SkillToolDescriptors.KEY_SKILL_TAGS));

        assertEquals(SkillToolDescriptors.TYPE_STRING, skillId.get(SkillToolDescriptors.KEY_TYPE));
        assertEquals(SkillToolDescriptors.TYPE_ARRAY, skillTags.get(SkillToolDescriptors.KEY_TYPE));
        assertThrows(UnsupportedOperationException.class, () -> schema.put("later", true));
    }

    @Test
    void honorsCustomInputSchemaFromSkillMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SkillMetadataKeys.KEY_INPUT_SCHEMA, Map.of(
                SkillToolDescriptors.KEY_TYPE, SkillToolDescriptors.TYPE_OBJECT,
                SkillToolDescriptors.KEY_PROPERTIES, Map.of(
                        "text", Map.of(SkillToolDescriptors.KEY_TYPE, SkillToolDescriptors.TYPE_STRING)),
                SkillToolDescriptors.KEY_REQUIRED, List.of("text")));

        Map<String, Object> schema = SkillToolDescriptors.inputSchema(skill(metadata));
        Map<?, ?> properties = assertInstanceOf(Map.class, schema.get(SkillToolDescriptors.KEY_PROPERTIES));

        assertEquals(List.of("text"), schema.get(SkillToolDescriptors.KEY_REQUIRED));
        assertInstanceOf(Map.class, properties.get("text"));
        assertThrows(UnsupportedOperationException.class, () -> schema.put("later", true));
        @SuppressWarnings("unchecked")
        Map<Object, Object> immutableProperties = (Map<Object, Object>) properties;
        assertThrows(UnsupportedOperationException.class, () -> immutableProperties.put("later", true));
    }

    @Test
    void mapsSkillDefinitionToToolMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SkillMetadataKeys.KEY_VERSION, "2.1.0");
        metadata.put(SkillMetadataKeys.KEY_CATEGORY, "retrieval");
        metadata.put(SkillMetadataKeys.KEY_TAGS, List.of("rag", "memory"));

        SkillDefinition skill = skill(metadata);
        SkillMetadata toolMetadata = SkillToolDescriptors.metadataFrom(skill);

        assertEquals("Summarize", SkillToolDescriptors.toolName(skill, toolMetadata));
        assertEquals("Condense text", SkillToolDescriptors.description(toolMetadata));
        assertEquals("2.1.0", toolMetadata.version());
        assertEquals("retrieval", toolMetadata.category());
        assertEquals(List.of("rag", "memory"), toolMetadata.tags());
    }

    @Test
    void fillsToolDescriptorFallbacksForSparseSkillDefinitions() {
        SkillDefinition skill = SkillDefinition.builder()
                .id("plain")
                .name(" ")
                .description(null)
                .systemPrompt("Run the skill")
                .metadata(Map.of())
                .build();
        SkillMetadata metadata = SkillToolDescriptors.metadataFrom(skill);

        assertEquals("plain", SkillToolDescriptors.toolName(skill, metadata));
        assertEquals("Skill plain", SkillToolDescriptors.description(metadata));
    }

    private static SkillDefinition skill(Map<String, Object> metadata) {
        return SkillDefinition.builder()
                .id("summarize")
                .name("Summarize")
                .description("Condense text")
                .category("test")
                .systemPrompt("Summarize the input")
                .metadata(metadata)
                .build();
    }
}
