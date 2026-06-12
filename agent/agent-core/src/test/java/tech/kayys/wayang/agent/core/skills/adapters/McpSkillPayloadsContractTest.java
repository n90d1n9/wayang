package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSkillPayloadsContractTest {

    @Test
    void mapsSkillDefinitionToImmutableMcpResource() {
        SkillDefinition skill = SkillDefinition.builder()
                .id("rag")
                .name("RAG")
                .description("Retrieve context")
                .category("retrieval")
                .systemPrompt("Use retrieval")
                .metadata(Map.of(
                        SkillMetadataKeys.KEY_TAGS, "rag memory",
                        SkillMetadataKeys.KEY_VERSION, "2.1.0"))
                .build();

        MCPSkillProvider.MCPSkillResource resource = McpSkillPayloads.resource(skill);

        assertEquals("rag", resource.id());
        assertEquals("RAG", resource.name());
        assertEquals("Retrieve context", resource.description());
        assertEquals("2.1.0", resource.version());
        assertEquals("retrieval", resource.metadata().get(SkillMetadataKeys.KEY_CATEGORY));
        assertEquals(List.of("rag", "memory"), resource.metadata().get(SkillMetadataKeys.KEY_TAGS));
        assertThrows(UnsupportedOperationException.class, () -> resource.metadata().put("later", true));
    }

    @Test
    void mapsSkillResultToStableMcpPayload() {
        SkillResult result = SkillResult.builder()
                .skillId("summarize")
                .status(SkillResult.Status.SUCCESS)
                .observation("Short answer")
                .output("tokens", 12)
                .build();

        Map<String, Object> payload = McpSkillPayloads.executionResult("summarize", result);

        assertEquals("summarize", payload.get(McpSkillPayloads.KEY_SKILL_ID));
        assertEquals(true, payload.get(McpSkillPayloads.KEY_SUCCESS));
        assertEquals("SUCCESS", payload.get(McpSkillPayloads.KEY_STATUS));
        assertEquals("Short answer", payload.get(McpSkillPayloads.KEY_RESULT));
        assertEquals(Map.of("tokens", 12), payload.get(McpSkillPayloads.KEY_OUTPUTS));
        assertThrows(UnsupportedOperationException.class, () -> payload.put("later", true));
    }

    @Test
    void mapsNullResultAndExceptionsToMcpErrorPayloads() {
        Map<String, Object> nullResult = McpSkillPayloads.executionResult("missing", null);
        Map<String, Object> exception = McpSkillPayloads.error("missing", new IllegalStateException("down"));

        assertEquals(false, nullResult.get(McpSkillPayloads.KEY_SUCCESS));
        assertEquals("ERROR", nullResult.get(McpSkillPayloads.KEY_STATUS));
        assertEquals(SkillResultPayloads.ERROR_NO_RESULT, nullResult.get(McpSkillPayloads.KEY_ERROR));
        assertEquals("down", exception.get(McpSkillPayloads.KEY_ERROR));
        assertFalse(exception.containsKey(McpSkillPayloads.KEY_RESULT));
    }
}
