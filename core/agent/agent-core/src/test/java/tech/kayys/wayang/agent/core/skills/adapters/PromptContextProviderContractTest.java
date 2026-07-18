package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptContextProviderContractTest {

    @Test
    void enrichesPromptWithActiveSkillMetadata() {
        SkillMetadata metadata = TestSkillContexts.metadata(
                "test-skill",
                "A test skill for prompt shaping",
                "2.1.0",
                "prompt",
                "demo");
        PromptContextProvider provider = new PromptContextProvider(TestSkillContexts.context("test-skill", metadata));

        String prompt = provider.enrichPrompt("Process this data:");

        assertTrue(prompt.startsWith("Process this data:"));
        assertTrue(prompt.contains("## Available Skill: test-skill"));
        assertTrue(prompt.contains("A test skill for prompt shaping"));
        assertTrue(prompt.contains("Tags: prompt, demo"));
    }

    @Test
    void exposesPromptVariablesAsImmutableSnapshot() {
        SkillMetadata metadata = TestSkillContexts.metadata("test-skill", "A test skill", "2.1.0", "prompt");
        PromptContextProvider provider = new PromptContextProvider(TestSkillContexts.context("test-skill", metadata))
                .withPromptContext(" template ", "{{input}}")
                .withPromptContext(null, "ignored")
                .withPromptContext("ignored", null);

        Map<String, String> variables = provider.getPromptVariables();

        assertEquals("{{input}}", variables.get("template"));
        assertEquals("test-skill", variables.get("skillId"));
        assertEquals("test-skill", variables.get("skillName"));
        assertEquals("A test skill", variables.get("skillDescription"));
        assertEquals("2.1.0", variables.get("skillVersion"));
        assertEquals("prompt", variables.get("skillTags"));
        assertFalse(variables.containsKey("ignored"));
        assertThrows(UnsupportedOperationException.class, () -> variables.put("new", "value"));
    }

    @Test
    void returnsPromptTemplateOnlyWhenNonBlank() {
        SkillContext context = TestSkillContexts.context("test-skill", null);
        PromptContextProvider provider = new PromptContextProvider(context);

        assertTrue(provider.getPromptTemplate().isEmpty());
        provider.withPromptContext("template", " ");
        assertTrue(provider.getPromptTemplate().isEmpty());
        provider.withPromptContext("template", "{{skill}} processing");
        assertEquals("{{skill}} processing", provider.getPromptTemplate().orElseThrow());
    }

    @Test
    void handlesMissingMetadataAndNullBasePrompt() {
        PromptContextProvider provider = new PromptContextProvider(TestSkillContexts.context("test-skill", null));

        assertEquals("", provider.enrichPrompt(null));
        assertEquals(Map.of("skillId", "test-skill"), provider.getPromptVariables());
    }

    @Test
    void enrichKeepsProviderFluent() {
        PromptContextProvider provider = new PromptContextProvider(TestSkillContexts.context("test-skill", null));

        assertSame(provider, provider.enrich().await().indefinitely());
    }
}
