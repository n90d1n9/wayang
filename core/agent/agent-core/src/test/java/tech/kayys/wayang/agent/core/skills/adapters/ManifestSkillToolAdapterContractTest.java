package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestSkillToolAdapterContractTest {

    @TempDir
    Path tempDir;

    @Test
    void projectsManifestInputSchemaToToolParameters() {
        ManifestSkillToolAdapter adapter = adapter(SkillManifest.builder()
                .name("summarize")
                .description("Use this skill for concise summary generation")
                .metadata(Map.of(
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "text", Map.of("type", "string", "description", "Text to summarize"),
                                        "style", Map.of("type", "string", "enum", List.of("brief", "detailed"))),
                                "required", List.of("text"),
                                "additionalProperties", false)))
                .build());

        Map<String, Object> parameters = adapter.getParameters();

        assertEquals("object", parameters.get("type"));
        assertEquals(List.of("text"), parameters.get("required"));
        assertEquals(false, parameters.get("additionalProperties"));
        Map<?, ?> properties = assertInstanceOf(Map.class, parameters.get("properties"));
        assertTrue(properties.containsKey("text"));
        assertTrue(properties.containsKey("style"));
    }

    @Test
    void ignoresPlainAllowedToolsAllowlistWhenProjectingParameters() {
        ManifestSkillToolAdapter adapter = adapter(SkillManifest.builder()
                .name("read-file")
                .description("Use this skill when reading workspace files")
                .allowedToolsString("read_file list_dir glob")
                .build());

        assertTrue(adapter.getParameters().isEmpty());
    }

    @Test
    void keepsLegacyJsonAllowedToolsSchemaAsCompatibilityFallback() {
        ManifestSkillToolAdapter adapter = adapter(SkillManifest.builder()
                .name("legacy")
                .description("Use this skill for legacy schema projection")
                .allowedToolsString("""
                        {
                          "type": "object",
                          "properties": { "path": { "type": "string" } },
                          "required": ["path"]
                        }
                        """)
                .build());

        Map<String, Object> parameters = adapter.getParameters();

        assertEquals("object", parameters.get("type"));
        assertEquals(List.of("path"), parameters.get("required"));
        Map<?, ?> properties = assertInstanceOf(Map.class, parameters.get("properties"));
        assertTrue(properties.containsKey("path"));
    }

    @Test
    void projectedSchemaIsImmutable() {
        ManifestSkillToolAdapter adapter = adapter(SkillManifest.builder()
                .name("search")
                .description("Use this skill for search requests")
                .metadata(Map.of(
                        "inputSchema", Map.of(
                                "query", Map.of("type", "string", "required", true))))
                .build());

        Map<String, Object> parameters = adapter.getParameters();
        Map<?, ?> properties = assertInstanceOf(Map.class, parameters.get("properties"));
        Map<?, ?> query = assertInstanceOf(Map.class, properties.get("query"));

        assertFalse(query.containsKey("required"));
        assertThrows(UnsupportedOperationException.class, () -> parameters.put("later", true));
        assertThrows(UnsupportedOperationException.class, () -> ((Map<Object, Object>) properties).put("later", true));
        assertThrows(UnsupportedOperationException.class, () -> ((Map<Object, Object>) query).put("later", true));
    }

    private ManifestSkillToolAdapter adapter(SkillManifest manifest) {
        return new ManifestSkillToolAdapter(manifest, new SkillExecutor(tempDir));
    }
}
