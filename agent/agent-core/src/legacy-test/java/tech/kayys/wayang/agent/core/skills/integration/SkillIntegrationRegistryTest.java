package tech.kayys.wayang.agent.core.skills.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.core.skills.adapters.*;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillIntegrationRegistry Tests")
class SkillIntegrationRegistryTest {

    private SkillIntegrationRegistry registry;
    private MockSkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        skillRegistry = new MockSkillRegistry();
        registry = new SkillIntegrationRegistry(skillRegistry);
    }

    @Test
    @DisplayName("Should initialize all integrations")
    void testInitialize() {
        SkillIntegrationRegistry initialized = registry.initialize()
            .await().indefinitely();

        assertSame(registry, initialized);
    }

    @Test
    @DisplayName("Should track integration status")
    void testGetIntegrationStatus() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();

        assertNotNull(status);
        assertTrue(status.containsKey("tools"));
        assertTrue(status.containsKey("prompt"));
        assertTrue(status.containsKey("memory"));
        assertTrue(status.containsKey("guardrails"));
        assertTrue(status.containsKey("hitl"));
        assertTrue(status.containsKey("rag"));
        assertTrue(status.containsKey("mcp"));
        assertTrue(status.containsKey("vector"));
    }

    @Test
    @DisplayName("Should provide tool adapters")
    void testGetToolAdapters() {
        registry.initialize().await().indefinitely();
        
        List<SkillAsToolAdapter> adapters = registry.getToolAdapters();

        assertNotNull(adapters);
        assertFalse(adapters.isEmpty());
    }

    @Test
    @DisplayName("Should initialize tool integration")
    void testToolIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("tools"));
    }

    @Test
    @DisplayName("Should initialize prompt integration")
    void testPromptIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("prompt"));
    }

    @Test
    @DisplayName("Should initialize memory integration")
    void testMemoryIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("memory"));
    }

    @Test
    @DisplayName("Should initialize guardrails integration")
    void testGuardrailsIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("guardrails"));
    }

    @Test
    @DisplayName("Should initialize HITL integration")
    void testHITLIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("hitl"));
    }

    @Test
    @DisplayName("Should initialize RAG integration")
    void testRAGIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("rag"));
    }

    @Test
    @DisplayName("Should initialize MCP integration")
    void testMCPIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("mcp"));
    }

    @Test
    @DisplayName("Should initialize vector integration")
    void testVectorIntegrationInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();
        
        assertEquals("initialized", status.get("vector"));
    }

    @Test
    @DisplayName("Should initialize all integrations in sequence")
    void testSequentialInitialization() {
        registry.initialize().await().indefinitely();
        
        Map<String, String> status = registry.getIntegrationStatus();

        // All should be initialized
        for (String value : status.values()) {
            assertEquals("initialized", value);
        }
    }

    @Test
    @DisplayName("Should support multiple initialize calls")
    void testMultipleInitializeCalls() {
        registry.initialize().await().indefinitely();
        
        // Second call should also succeed
        SkillIntegrationRegistry reinitialized = registry.initialize()
            .await().indefinitely();

        assertSame(registry, reinitialized);
    }

    // Mock skill registry for testing
    private static class MockSkillRegistry implements SkillRegistry {
        @Override
        public List<SkillDefinition> list() {
            return List.of(
                new MockSkillDefinition("skill-1", "Skill One", "First skill"),
                new MockSkillDefinition("skill-2", "Skill Two", "Second skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<SkillDefinition> get(String skillId) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new MockSkillDefinition(skillId, "Test Skill", "A test skill")
            );
        }

        @Override
        public io.smallrye.mutiny.Uni<SkillResult> executeSkill(
            String skillId,
            Map<String, Object> input) {
            return io.smallrye.mutiny.Uni.createFrom().item(
                new SkillResult(
                    skillId,
                    "invoc-123",
                    SkillResult.Status.SUCCESS,
                    "Executed: " + skillId,
                    true
                )
            );
        }
    }

    private static class MockSkillDefinition implements SkillDefinition {
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
