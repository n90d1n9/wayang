package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MCPSkillProvider Tests")
class MCPSkillProviderTest {

    private MCPSkillProvider provider;
    private MockSkillRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = new MockSkillRegistry();
        provider = new MCPSkillProvider(mockRegistry);
    }

    @Test
    @DisplayName("Should configure endpoint via fluent API")
    void testConfigureEndpoint() {
        MCPSkillProvider result = provider.withEndpoint("http://localhost:8080/mcp");
        
        assertSame(provider, result);
        
        Map<String, Object> config = provider.getConfiguration();
        assertTrue(config.containsKey("endpoint"));
        assertEquals("http://localhost:8080/mcp", config.get("endpoint"));
    }

    @Test
    @DisplayName("Should configure protocol version")
    void testConfigureProtocolVersion() {
        MCPSkillProvider result = provider.withProtocolVersion("1.0");
        
        assertSame(provider, result);
        
        Map<String, Object> config = provider.getConfiguration();
        assertTrue(config.containsKey("protocol_version"));
        assertEquals("1.0", config.get("protocol_version"));
    }

    @Test
    @DisplayName("Should list skills as MCP resources")
    void testListSkillsAsResources() {
        var resources = provider.listSkillsAsResources().await().indefinitely();
        
        assertNotNull(resources);
        assertEquals(2, resources.size());
        
        MCPSkillProvider.MCPSkillResource resource1 = resources.get(0);
        assertEquals("skill-1", resource1.id());
        assertEquals("Skill One", resource1.name());
    }

    @Test
    @DisplayName("Should execute skill via MCP protocol")
    void testExecuteViaMCP() {
        Map<String, Object> input = Map.of("param", "value");
        
        Map<String, Object> result = provider.executeViaMCP("skill-1", input)
            .await().indefinitely();

        assertNotNull(result);
        assertTrue(result.containsKey("skill_id"));
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("result"));
        assertTrue(result.containsKey("status"));
        
        assertEquals("skill-1", result.get("skill_id"));
    }

    @Test
    @DisplayName("Should initialize provider")
    void testInitialize() {
        MCPSkillProvider initialized = provider.initialize().await().indefinitely();
        
        assertSame(provider, initialized);
    }

    @Test
    @DisplayName("Should support fluent configuration chain")
    void testFluentChain() {
        provider
            .withEndpoint("http://localhost:8080")
            .withProtocolVersion("1.0");

        Map<String, Object> config = provider.getConfiguration();
        
        assertEquals("http://localhost:8080", config.get("endpoint"));
        assertEquals("1.0", config.get("protocol_version"));
    }

    @Test
    @DisplayName("Should convert skill to MCP resource")
    void testSkillToMCPResource() {
        var resources = provider.listSkillsAsResources().await().indefinitely();
        
        MCPSkillProvider.MCPSkillResource resource = resources.get(0);
        
        assertEquals("skill-1", resource.id());
        assertEquals("Skill One", resource.name());
        assertFalse(resource.description().isEmpty());
        assertFalse(resource.version().isEmpty());
        assertTrue(resource.metadata().containsKey("tags"));
    }

    @Test
    @DisplayName("Should handle empty skills list")
    void testEmptySkillsList() {
        MockSkillRegistry emptyRegistry = new MockSkillRegistry();
        MCPSkillProvider emptyProvider = new MCPSkillProvider(emptyRegistry);
        
        var resources = emptyProvider.listSkillsAsResources().await().indefinitely();
        
        assertTrue(resources.isEmpty());
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
            java.util.Map<String, Object> input) {
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
