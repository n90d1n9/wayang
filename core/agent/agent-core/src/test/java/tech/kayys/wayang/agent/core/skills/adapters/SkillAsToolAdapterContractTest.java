package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillAsToolAdapterContractTest {

    @Test
    void adaptsRegisteredSkillsToToolSpi() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill);

        List<SkillAsToolAdapter> tools = SkillAsToolAdapter.adaptSkills(registry);

        assertEquals(1, tools.size());
        Tool tool = tools.getFirst();
        assertEquals("summarize", tool.id());
        assertEquals("Summarize", tool.name());
        assertEquals("Condense text", tool.description());
        assertEquals("object", tool.inputSchema().get("type"));
        assertEquals(List.of("skillId"), tool.inputSchema().get("required"));
    }

    @Test
    void executesSkillThroughToolBoundary() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .result("summarize", SkillResult.builder()
                        .skillId("summarize")
                        .status(SkillResult.Status.SUCCESS)
                        .observation("Short answer")
                        .output("tokens", 12)
                        .build());
        SkillAsToolAdapter adapter = SkillAsToolAdapter.adaptSkills(registry).getFirst();

        ToolResult result = adapter.execute(Map.of("text", "long"), ToolContext.defaults());

        assertTrue(result.success());
        Map<?, ?> data = assertInstanceOf(Map.class, result.data());
        assertEquals("Short answer", data.get(SkillResultPayloads.KEY_OBSERVATION));
        assertEquals("SUCCESS", data.get(SkillResultPayloads.KEY_STATUS));
        assertEquals(Map.of("tokens", 12), data.get(SkillResultPayloads.KEY_OUTPUTS));
    }

    @Test
    void mapsSkillFailureResultToToolError() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .result("summarize", SkillResult.failure("summarize", "Input was invalid"));
        SkillAsToolAdapter adapter = SkillAsToolAdapter.adaptSkills(registry).getFirst();

        ToolResult result = adapter.execute(Map.of("text", "bad"), ToolContext.defaults());

        assertFalse(result.success());
        assertEquals("Input was invalid", result.error());
    }

    @Test
    void mapsRegistryFailureToAsyncToolError() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .failExecution("summarize", new IllegalStateException("Registry unavailable"));
        SkillAsToolAdapter adapter = SkillAsToolAdapter.adaptSkills(registry).getFirst();

        ToolResult result = adapter.executeAsync(Map.of("text", "long"), ToolContext.defaults())
                .await().indefinitely();

        assertFalse(result.success());
        assertEquals("Registry unavailable", result.error());
    }

    @Test
    void mapsSkillFailureThrowableToToolErrorWhenObservationIsMissing() {
        SkillDefinition skill = TestSkillRegistry.skill("summarize", "Summarize", "Condense text");
        TestSkillRegistry registry = TestSkillRegistry.of(skill)
                .result("summarize", SkillResult.builder()
                        .skillId("summarize")
                        .status(SkillResult.Status.ERROR)
                        .error(new IllegalStateException("Skill backend down"))
                        .build());
        SkillAsToolAdapter adapter = SkillAsToolAdapter.adaptSkills(registry).getFirst();

        ToolResult result = adapter.execute(Map.of("text", "bad"), ToolContext.defaults());

        assertFalse(result.success());
        assertEquals("Skill backend down", result.error());
    }
}
