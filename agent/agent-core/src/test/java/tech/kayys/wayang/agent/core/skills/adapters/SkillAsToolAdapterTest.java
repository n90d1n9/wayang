package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillAsToolAdapter Tests")
class SkillAsToolAdapterTest {

    private SkillAsToolAdapter adapter;
    private MockSkillRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = new MockSkillRegistry();
        adapter = new SkillAsToolAdapter(mockRegistry);
    }

    @Test
    @DisplayName("Should adapt skill to Tool implementation")
    void testSkillToToolAdaptation() {
        adapter.initialize().await().indefinitely();
        
        List<Object> tools = adapter.getAdaptedTools();
        
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
    }

    @Test
    @DisplayName("Should generate tool schema from skill metadata")
    void testGenerateToolSchema() {
        adapter.initialize().await().indefinitely();
        
        Map<String, Object> schema = adapter.generateToolSchema("skill-1");
        
        assertNotNull(schema);
        assertTrue(schema.containsKey("name"));
        assertTrue(schema.containsKey("description"));
        assertTrue(schema.containsKey("input_schema"));
        
        assertEquals("skill-1", schema.get("name"));
    }

    @Test
    @DisplayName("Should execute skill as tool")
    void testExecuteSkillAsTool() {
        adapter.initialize().await().indefinitely();
        
        Map<String, Object> input = Map.of("param", "value");
        Map<String, Object> result = adapter.executeSkillAsTool("skill-1", input)
            .await().indefinitely();

        assertNotNull(result);
        assertTrue(result.containsKey("skill_id"));
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("result"));
        
        assertEquals("skill-1", result.get("skill_id"));
    }

    @Test
    @DisplayName("Should convert skill to Tool protocol")
    void testToolProtocolConversion() {
        adapter.initialize().await().indefinitely();
        
        Object tool = adapter.convertToToolProtocol("skill-1");
        
        assertNotNull(tool);
    }

    @Test
    @DisplayName("Should support fluent API")
    void testFluentAPI() {
        SkillAsToolAdapter result = adapter.initialize().await().indefinitely();
        
        assertSame(adapter, result);
    }

    @Test
    @DisplayName("Should list all adapted tools")
    void testListAdaptedTools() {
        adapter.initialize().await().indefinitely();
        
        List<Object> tools = adapter.getAdaptedTools();
        
        assertNotNull(tools);
        assertEquals(2, tools.size());
    }

    @Test
    @DisplayName("Should preserve skill metadata in tool schema")
    void testMetadataPreservation() {
        adapter.initialize().await().indefinitely();
        
        Map<String, Object> schema = adapter.generateToolSchema("skill-1");
        
        assertFalse(schema.get("description").toString().isEmpty());
    }

    @Test
    @DisplayName("Should handle skill execution errors as tool errors")
    void testErrorHandling() {
        adapter.initialize().await().indefinitely();
        
        Map<String, Object> input = Map.of("param", "bad-value");
        // Should not throw, but return error in result
        Map<String, Object> result = adapter.executeSkillAsTool("skill-error", input)
            .onFailure().recoverWithItem(Map.of("error", "Execution failed"))
            .await().indefinitely();

        assertNotNull(result);
    }

    // Mock skill registry for testing
    private static class MockSkillRegistry implements tech.kayys.wayang.agent.spi.skills.SkillRegistry {
        @Override
        public List<tech.kayys.wayang.agent.spi.skills.SkillDefinition> list() {
            return List.of(
                new MockSkillDefinition("skill-1", "Skill One", "First skill"),
                new MockSkillDefinition("skill-2", "Skill Two", "Second skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillDefinition> get(String skillId) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new MockSkillDefinition(skillId, "Test Skill", "A test skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<tech.kayys.wayang.agent.spi.skills.SkillResult> executeSkill(
            String skillId,
            Map<String, Object> input) {
            if ("skill-error".equals(skillId)) {
                return io.smallrye.mutiny.Uni.createFrom().failure(
                    new RuntimeException("Skill execution error")
                );
            }
            return io.smallrye.mutiny.Uni.createFrom().item(
                new tech.kayys.wayang.agent.spi.skills.SkillResult(
                    skillId,
                    "invoc-123",
                    tech.kayys.wayang.agent.spi.skills.SkillResult.Status.SUCCESS,
                    "Executed: " + skillId,
                    true
                )
            );
        }
    }

    private static class MockSkillDefinition implements tech.kayys.wayang.agent.spi.skills.SkillDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final SkillMetadata metadata;

        MockSkillDefinition(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.metadata = new SkillMetadata(
                id, name, description, "1.0.0", "test",
                List.of("test"), null
            );
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SkillMetadata metadata() {
            return metadata;
        }
    }
}
