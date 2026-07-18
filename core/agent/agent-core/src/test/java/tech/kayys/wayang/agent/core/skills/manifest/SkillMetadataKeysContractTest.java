package tech.kayys.wayang.agent.core.skills.manifest;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMetadataKeysContractTest {

    @Test
    void readsCommonMetadataFieldsWithDefaults() {
        Map<String, Object> metadata = Map.of(
                SkillMetadataKeys.KEY_CATEGORY, "rag",
                SkillMetadataKeys.KEY_DOMAINS, List.of("search", "analysis"),
                SkillMetadataKeys.KEY_OUTPUT_FORMAT, "json",
                SkillMetadataKeys.KEY_TAGS, "retrieval, memory search",
                SkillMetadataKeys.KEY_VERSION, "2.0.0");

        assertEquals("rag", SkillMetadataKeys.category(metadata));
        assertEquals(List.of("search", "analysis"), SkillMetadataKeys.domains(metadata));
        assertEquals("json", SkillMetadataKeys.outputFormat(metadata).orElseThrow());
        assertEquals(List.of("retrieval", "memory", "search"), SkillMetadataKeys.tags(metadata));
        assertEquals("2.0.0", SkillMetadataKeys.version(metadata));
    }

    @Test
    void fallsBackForMissingOrUnexpectedMetadataShapes() {
        Map<String, Object> metadata = Map.of(
                SkillMetadataKeys.KEY_CATEGORY, 17,
                SkillMetadataKeys.KEY_TAGS, 23,
                SkillMetadataKeys.KEY_VERSION, "");

        assertEquals(SkillMetadataKeys.DEFAULT_CATEGORY, SkillMetadataKeys.category(metadata));
        assertEquals("fallback", SkillMetadataKeys.category(Map.of(), "fallback"));
        assertTrue(SkillMetadataKeys.tags(metadata).isEmpty());
        assertEquals(SkillMetadataKeys.DEFAULT_VERSION, SkillMetadataKeys.version(metadata));
    }

    @Test
    void skillMetadataDelegatesToSharedKeys() {
        SkillMetadata metadata = SkillMetadata.builder()
                .name("demo")
                .description("Demo skill")
                .allowedTools("shell browser")
                .customMetadata(Map.of(
                        SkillMetadataKeys.KEY_CATEGORY, "filesystem",
                        SkillMetadataKeys.KEY_VERSION, "1.2.3"))
                .build();

        assertEquals("filesystem", metadata.category());
        assertEquals("1.2.3", metadata.version());
        assertEquals(List.of("shell", "browser"), metadata.tags());
    }
}
