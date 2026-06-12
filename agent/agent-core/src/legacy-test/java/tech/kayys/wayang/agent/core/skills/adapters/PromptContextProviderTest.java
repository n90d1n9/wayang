package tech.kayys.wayang.agent.core.skills.adapters;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import tech.kayys.wayang.agent.spi.skills.SkillResult.Status;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromptContextProvider Tests")
class PromptContextProviderTest {

    private SkillContext mockContext;
    private SkillMetadata mockMetadata;
    private PromptContextProvider provider;

    @BeforeEach
    void setUp() {
        mockMetadata = new SkillMetadata(
            "test-skill",
            "Test Skill",
            "A test skill for unit testing",
            "1.0.0",
            "test",
            List.of("test", "demo"),
            null
        );

        mockContext = new SkillContext() {
            @Override
            public String skillId() {
                return "test-skill";
            }

            @Override
            public String userId() {
                return "test-user";
            }

            @Override
            public SkillMetadata metadata() {
                return mockMetadata;
            }

            @Override
            public Map<String, Object> variables() {
                return Map.of("var1", "value1");
            }

            @Override
            public long timeoutMs() {
                return 5000;
            }
        };

        provider = new PromptContextProvider(mockContext);
    }

    @Test
    @DisplayName("Should enrich prompt with skill metadata")
    void testEnrichPrompt() {
        String basePrompt = "Process this data:";
        String enriched = provider.enrichPrompt(basePrompt);

        assertNotNull(enriched);
        assertTrue(enriched.contains("test-skill"));
        assertTrue(enriched.contains("Test Skill"));
        assertTrue(enriched.contains("A test skill"));
        assertTrue(enriched.contains("test"));
        assertTrue(enriched.contains("demo"));
    }

    @Test
    @DisplayName("Should extract prompt variables correctly")
    void testGetPromptVariables() {
        provider.withPromptContext("template", "custom-template");
        
        Map<String, String> variables = provider.getPromptVariables();

        assertNotNull(variables);
        assertTrue(variables.containsKey("skillName"));
        assertEquals("Test Skill", variables.get("skillName"));
        assertTrue(variables.containsKey("skillDescription"));
        assertTrue(variables.containsKey("skillVersion"));
        assertTrue(variables.containsKey("template"));
    }

    @Test
    @DisplayName("Should return prompt template when set")
    void testGetPromptTemplate() {
        provider.withPromptContext("template", "{{skill}} processing");
        
        var template = provider.getPromptTemplate();
        
        assertTrue(template.isPresent());
        assertEquals("{{skill}} processing", template.get());
    }

    @Test
    @DisplayName("Should enrich asynchronously")
    void testEnrichAsync() {
        Uni<PromptContextProvider> enriched = provider.enrich();
        
        assertNotNull(enriched);
        PromptContextProvider result = enriched.await().indefinitely();
        assertSame(provider, result);
    }

    @Test
    @DisplayName("Should add prompt context via fluent API")
    void testFluentAPI() {
        PromptContextProvider result = provider
            .withPromptContext("key1", "value1")
            .withPromptContext("key2", "value2");

        assertSame(provider, result);
        
        Map<String, String> variables = provider.getPromptVariables();
        assertTrue(variables.containsKey("key1"));
        assertTrue(variables.containsKey("key2"));
    }
}
