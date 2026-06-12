package tech.kayys.wayang.agent.core.skills.integration;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillManifestRuntimeSkillMapperContractTest {

    @Test
    void mapsFilesystemManifestToRuntimeSkillDefinition() {
        SkillManifest manifest = SkillManifest.builder()
                .name("rag-search")
                .description("Search documents")
                .version("2.0.0")
                .license("MIT")
                .compatibility("java")
                .allowedTools(List.of("read-file", "search"))
                .metadata(Map.of(
                        SkillMetadataKeys.KEY_CATEGORY, "retrieval",
                        SkillMetadataKeys.KEY_TAGS, List.of("rag", "search"),
                        SkillMetadataKeys.KEY_INPUT_SCHEMA, Map.of(
                                "type", "object",
                                "properties", Map.of("query", Map.of("type", "string")),
                                "required", List.of("query"))))
                .bodyContent("Use retrieval before answering.")
                .sourceDirectory(Path.of("/tmp/skills/rag-search"))
                .build();

        SkillDefinition skill = new SkillManifestRuntimeSkillMapper().toSkillDefinition(manifest);

        assertEquals("rag-search", skill.id());
        assertEquals("rag-search", skill.name());
        assertEquals("Search documents", skill.description());
        assertEquals("retrieval", skill.category());
        assertEquals("Use retrieval before answering.", skill.systemPrompt());
        assertEquals(List.of("read-file", "search"), skill.tools());
        assertEquals("2.0.0", skill.metadata().get(SkillMetadataKeys.KEY_VERSION));
        assertEquals("MIT", skill.metadata().get(SkillManifestRuntimeSkillMapper.KEY_LICENSE));
        assertEquals("java", skill.metadata().get(SkillManifestRuntimeSkillMapper.KEY_COMPATIBILITY));
        assertEquals("/tmp/skills/rag-search",
                skill.metadata().get(SkillManifestRuntimeSkillMapper.KEY_SOURCE_DIRECTORY));
        assertTrue((Boolean) skill.metadata().get(SkillManifestRuntimeSkillMapper.KEY_FILESYSTEM_SKILL));
        assertTrue(skill.metadata().get(SkillMetadataKeys.KEY_INPUT_SCHEMA) instanceof Map<?, ?>);
    }

    @Test
    void usesSafeFallbacksForSparseManifests() {
        SkillManifest manifest = SkillManifest.builder()
                .name("empty-body")
                .description("Use this sparse manifest")
                .build();

        SkillDefinition skill = new SkillManifestRuntimeSkillMapper().toSkillDefinition(manifest);

        assertEquals(SkillManifestRuntimeSkillMapper.CATEGORY_FILESYSTEM, skill.category());
        assertEquals("Use this sparse manifest", skill.systemPrompt());
        assertTrue(skill.tools().isEmpty());
    }

    @Test
    void requiresManifest() {
        assertThrows(NullPointerException.class,
                () -> new SkillManifestRuntimeSkillMapper().toSkillDefinition(null));
    }
}
