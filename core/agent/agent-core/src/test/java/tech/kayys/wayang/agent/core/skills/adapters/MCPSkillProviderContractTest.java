package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MCPSkillProviderContractTest {

    @Test
    void exposesSkillsAsMcpResources() {
        SkillDefinition summarize = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        SkillDefinition retrieve = TestSkillRegistry.skill("retrieve", "Retrieve", "Find documents");
        MCPSkillProvider provider = new MCPSkillProvider(TestSkillRegistry.of(summarize, retrieve));

        List<MCPSkillProvider.MCPSkillResource> resources = provider.listSkillsAsResources()
                .await().indefinitely();

        assertEquals(2, resources.size());
        MCPSkillProvider.MCPSkillResource resource = resources.getFirst();
        assertEquals("summarize", resource.id());
        assertEquals("Summarize", resource.name());
        assertEquals("Condense text", resource.description());
        assertEquals("1.0.0", resource.version());
        assertEquals("test", resource.metadata().get(SkillMetadataKeys.KEY_CATEGORY));
        assertEquals(List.of(), resource.metadata().get(SkillMetadataKeys.KEY_TAGS));
    }

    @Test
    void supportsFluentConfigurationWithSnapshotReadback() {
        MCPSkillProvider provider = new MCPSkillProvider(TestSkillRegistry.of())
                .withEndpoint("http://localhost:8080/mcp")
                .withProtocolVersion("2025-06-18");

        Map<String, Object> configuration = provider.getConfiguration();
        configuration.put(McpSkillPayloads.KEY_ENDPOINT, "mutated");

        assertSame(provider, provider.initialize().await().indefinitely());
        assertEquals("http://localhost:8080/mcp", provider.getConfiguration().get(McpSkillPayloads.KEY_ENDPOINT));
        assertEquals("2025-06-18", provider.getConfiguration().get(McpSkillPayloads.KEY_PROTOCOL_VERSION));
    }

    @Test
    void executesSkillThroughMcpBoundary() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .result("summarize", SkillResult.builder()
                        .skillId("summarize")
                        .status(SkillResult.Status.SUCCESS)
                        .observation("Short answer")
                        .output("tokens", 12)
                        .build());
        MCPSkillProvider provider = new MCPSkillProvider(registry);

        Map<String, Object> result = provider.executeViaMCP("summarize", Map.of("text", "long"))
                .await().indefinitely();

        assertEquals("summarize", result.get(McpSkillPayloads.KEY_SKILL_ID));
        assertEquals(true, result.get(McpSkillPayloads.KEY_SUCCESS));
        assertEquals("SUCCESS", result.get(McpSkillPayloads.KEY_STATUS));
        assertEquals("Short answer", result.get(McpSkillPayloads.KEY_RESULT));
        assertEquals(Map.of("tokens", 12), result.get(McpSkillPayloads.KEY_OUTPUTS));
    }

    @Test
    void returnsStructuredMcpErrorForSkillFailureResult() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .result("summarize", SkillResult.failure("summarize", "Input was invalid"));
        MCPSkillProvider provider = new MCPSkillProvider(registry);

        Map<String, Object> result = provider.executeViaMCP("summarize", null)
                .await().indefinitely();

        assertEquals("summarize", result.get(McpSkillPayloads.KEY_SKILL_ID));
        assertEquals(false, result.get(McpSkillPayloads.KEY_SUCCESS));
        assertEquals("FAILURE", result.get(McpSkillPayloads.KEY_STATUS));
        assertEquals("Input was invalid", result.get(McpSkillPayloads.KEY_RESULT));
        assertFalse(result.containsKey(McpSkillPayloads.KEY_ERROR));
    }

    @Test
    void recoversRegistryExceptionAsMcpErrorPayload() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .failExecution("summarize", new IllegalStateException("Registry unavailable"));
        MCPSkillProvider provider = new MCPSkillProvider(registry);

        Map<String, Object> result = provider.executeViaMCP("summarize", Map.of())
                .await().indefinitely();

        assertEquals("summarize", result.get(McpSkillPayloads.KEY_SKILL_ID));
        assertEquals(false, result.get(McpSkillPayloads.KEY_SUCCESS));
        assertEquals("ERROR", result.get(McpSkillPayloads.KEY_STATUS));
        assertEquals("Registry unavailable", result.get(McpSkillPayloads.KEY_ERROR));
        assertTrue(result.containsKey(McpSkillPayloads.KEY_ERROR));
    }
}
