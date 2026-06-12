package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.context.ProjectContext;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.tool.BuiltInTools;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link PromptBuilder} — the most critical component since every
 * agent turn flows through it.
 */
@ExtendWith(MockitoExtension.class)
class PromptBuilderTest {

    @Mock BuiltInTools    builtInTools;
    @Mock ProjectContext  projectContext;
    @Mock AgentMemory     memory;
    @InjectMocks PromptBuilder builder;

    @BeforeEach
    void setUp() {
        when(builtInTools.describeAll()).thenReturn("`read_file` — Read a file.\n`write_file` — Write a file.\n");
        when(projectContext.contextBlock()).thenReturn("");
        when(memory.promptBlock()).thenReturn("");
    }

    @Test
    void systemPromptContainsIdentitySection() {
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).contains("Gamelan");
        assertThat(prompt).contains("Operating Principles");
    }

    @Test
    void systemPromptContainsToolProtocol() {
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).contains("tool_call");
        assertThat(prompt).contains("tool_result");
        assertThat(prompt).contains("STOP");
    }

    @Test
    void systemPromptContainsToolCatalogue() {
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).contains("read_file");
        assertThat(prompt).contains("write_file");
    }

    @Test
    void toolCatalogueIsCachedAfterFirstCall() {
        builder.buildSystemPrompt(List.of());
        builder.buildSystemPrompt(List.of());
        // describeAll should only be called once — cached
        verify(builtInTools, times(1)).describeAll();
    }

    @Test
    void systemPromptInjectsProjectContext() {
        when(projectContext.contextBlock()).thenReturn("## Project Context\nmaven / Java\n");
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).contains("Project Context");
        assertThat(prompt).contains("maven / Java");
    }

    @Test
    void systemPromptInjectsMemoryBlock() {
        when(memory.promptBlock()).thenReturn("## Remembered Context\n- test-cmd = mvn test\n");
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).contains("Remembered Context");
        assertThat(prompt).contains("mvn test");
    }

    @Test
    void systemPromptInjectsSkillInstructions() {
        Skill skill = new Skill("read-file", "Read files", "", "", Map.of(),
                List.of(), "## Read File\nAlways use line ranges.", "raw",
                Map.of(), List.of(), null);

        String prompt = builder.buildSystemPrompt(List.of(skill));

        assertThat(prompt).contains("read-file");
        assertThat(prompt).contains("Always use line ranges.");
    }

    @Test
    void skillReferencesAreIncluded() {
        Skill skill = new Skill("analyze-code", "Analyze code", "", "", Map.of(),
                List.of(), "Instructions here.", "raw",
                Map.of("REFERENCE.md", "## Patterns\n- Use records"), List.of(), null);

        String prompt = builder.buildSystemPrompt(List.of(skill));
        assertThat(prompt).contains("REFERENCE.md");
        assertThat(prompt).contains("Use records");
    }

    @Test
    void skillReferencesAreTruncatedAt3000Chars() {
        String bigRef = "x".repeat(5000);
        Skill skill = new Skill("big-skill", "Has big ref", "", "", Map.of(),
                List.of(), "Body.", "raw",
                Map.of("big.md", bigRef), List.of(), null);

        String prompt = builder.buildSystemPrompt(List.of(skill));
        // The reference should be truncated
        assertThat(prompt).contains("truncated");
        assertThat(prompt.length()).isLessThan(bigRef.length() + 2000);
    }

    @Test
    void minimalPromptOmitsSkills() {
        String minimal = builder.buildMinimalPrompt();
        assertThat(minimal).contains("Gamelan");
        assertThat(minimal).contains("tool_call");
        // Should not have skill section header
        assertThat(minimal).doesNotContain("Active Skill Guides");
    }

    @Test
    void noSkillsProducesNoSkillSection() {
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).doesNotContain("Active Skill Guides");
    }

    @Test
    void systemPromptContainsRememberProtocol() {
        String prompt = builder.buildSystemPrompt(List.of());
        assertThat(prompt).containsIgnoringCase("REMEMBER");
    }

    @Test
    void systemPromptContainsCurrentDate() {
        String prompt = builder.buildSystemPrompt(List.of());
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        assertThat(prompt).contains(year);
    }

    @Test
    void multipleSkillsAreAllIncluded() {
        Skill s1 = new Skill("skill-a", "Desc A", "", "", Map.of(), List.of(),
                "Instructions A.", "raw", Map.of(), List.of(), null);
        Skill s2 = new Skill("skill-b", "Desc B", "", "", Map.of(), List.of(),
                "Instructions B.", "raw", Map.of(), List.of(), null);

        String prompt = builder.buildSystemPrompt(List.of(s1, s2));
        assertThat(prompt).contains("skill-a");
        assertThat(prompt).contains("Instructions A.");
        assertThat(prompt).contains("skill-b");
        assertThat(prompt).contains("Instructions B.");
    }
}
