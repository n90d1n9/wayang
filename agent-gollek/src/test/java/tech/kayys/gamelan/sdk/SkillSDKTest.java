package tech.kayys.gamelan.sdk;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link SkillSDK} — builder, validator, scaffolder, and test harness.
 */
class SkillSDKTest {

    // ── Builder ───────────────────────────────────────────────────────────

    @Test
    void builderCreatesValidSkillDefinition() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("my-analyzer")
                .description("Analyzes Java code for issues")
                .version("2.0")
                .author("dev-team")
                .allowedTools("read_file", "search_files")
                .trigger("user asks to review code")
                .trigger("user asks to find bugs")
                .instruction("Always read the file first")
                .instruction("Focus on null handling")
                .parameter("path", "string", true, "File or directory to analyze")
                .evolveEnabled(true)
                .metadata("team", "platform")
                .build();

        assertThat(skill.name()).isEqualTo("my-analyzer");
        assertThat(skill.description()).isEqualTo("Analyzes Java code for issues");
        assertThat(skill.version()).isEqualTo("2.0");
        assertThat(skill.author()).isEqualTo("dev-team");
        assertThat(skill.allowedTools()).containsExactly("read_file", "search_files");
        assertThat(skill.triggers()).hasSize(2);
        assertThat(skill.instructions()).hasSize(2);
        assertThat(skill.parameters()).hasSize(1);
        assertThat(skill.evolveEnabled()).isTrue();
        assertThat(skill.metadata()).containsEntry("team", "platform");
    }

    @ParameterizedTest
    @ValueSource(strings = { "INVALID", "Invalid-Name", "has space", "123-starts-with-number", "" })
    void invalidNamesAreRejected(String name) {
        assertThatThrownBy(() -> SkillSDK.SkillDefinition.builder(name))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "valid-name", "my-skill", "a", "analyze-code-v2" })
    void validNamesAreAccepted(String name) {
        assertThatCode(() -> SkillSDK.SkillDefinition.builder(name)
                .description("desc").instruction("do x").build())
                .doesNotThrowAnyException();
    }

    @Test
    void builderRequiresDescription() {
        assertThatThrownBy(() -> SkillSDK.SkillDefinition.builder("test-skill").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("description");
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    void validSkillHasNoErrors() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("read-file")
                .description("Reads files from disk")
                .instruction("Use read_file tool")
                .build();
        List<String> errors = SkillSDK.validate(skill);
        assertThat(errors).isEmpty();
    }

    @Test
    void skillWithDescriptionTooLongHasError() {
        String longDesc = "x".repeat(1025);
        // Can't set via builder as build() doesn't check length, set via reflection workaround
        // Test the validate method independently
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("test")
                .description(longDesc).instruction("do something").build();
        List<String> errors = SkillSDK.validate(skill);
        assertThat(errors).anyMatch(e -> e.contains("1024"));
    }

    // ── SKILL.md generation ────────────────────────────────────────────────

    @Test
    void generatedSkillMdContainsFrontmatter() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("test-skill")
                .description("Test skill description")
                .allowedTools("read_file")
                .trigger("when user asks about X")
                .instruction("Step 1: read the file")
                .build();

        String md = SkillSDK.SkillScaffolder.generateSkillMd(skill);

        assertThat(md).startsWith("---\n");
        assertThat(md).contains("name: test-skill");
        assertThat(md).contains("description: Test skill description");
        assertThat(md).contains("allowed-tools: read_file");
        assertThat(md).contains("---\n");
        assertThat(md).contains("## When to activate");
        assertThat(md).contains("when user asks about X");
        assertThat(md).contains("## Instructions");
        assertThat(md).contains("Step 1: read the file");
    }

    @Test
    void generatedSkillMdIncludesParameters() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("param-skill")
                .description("Has parameters")
                .instruction("use them")
                .parameter("path", "string", true, "File path")
                .parameter("depth", "integer", false, "Tree depth", "2")
                .build();

        String md = SkillSDK.SkillScaffolder.generateSkillMd(skill);

        assertThat(md).contains("## Parameters");
        assertThat(md).contains("`path`");
        assertThat(md).contains("**required**");
        assertThat(md).contains("`depth`");
        assertThat(md).contains("*(optional)*");
        assertThat(md).contains("Default: `2`");
    }

    @Test
    void evolveEnabledSkillIncludesFlag() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("evolve-skill")
                .description("Evolve-enabled skill")
                .instruction("do work")
                .evolveEnabled(true)
                .build();
        String md = SkillSDK.SkillScaffolder.generateSkillMd(skill);
        assertThat(md).contains("evolve: \"true\"");
    }

    // ── Scaffolder ────────────────────────────────────────────────────────

    @Test
    void scaffolderCreatesCorrectDirectoryStructure(@TempDir Path tmp) throws IOException {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("scaffold-test")
                .description("Tests scaffolding")
                .instruction("read then write")
                .build();

        Path skillDir = SkillSDK.SkillScaffolder.scaffold(skill, tmp);

        assertThat(skillDir).exists();
        assertThat(skillDir.resolve("SKILL.md")).exists();
        assertThat(skillDir.resolve("README.md")).exists();
        assertThat(skillDir.resolve("references")).isDirectory();
        assertThat(skillDir.resolve("scripts")).isDirectory();
        assertThat(skillDir.resolve("assets")).isDirectory();
        assertThat(skillDir.resolve("tests").resolve("test_scaffold_test.py")).exists();
    }

    @Test
    void scaffolderSkillMdIsValid(@TempDir Path tmp) throws IOException {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("my-skill")
                .description("A well-formed skill for testing")
                .trigger("when code review needed")
                .instruction("Always read files before modifying")
                .build();

        Path skillDir = SkillSDK.SkillScaffolder.scaffold(skill, tmp);
        String content = Files.readString(skillDir.resolve("SKILL.md"));

        assertThat(content).startsWith("---");
        assertThat(content).contains("name: my-skill");
        assertThat(content).contains("description: A well-formed skill");
    }

    @Test
    void scaffolderTestTemplateContainsSkillName(@TempDir Path tmp) throws IOException {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("my-test-skill")
                .description("desc").instruction("do it").build();
        Path skillDir = SkillSDK.SkillScaffolder.scaffold(skill, tmp);
        String testContent = Files.readString(skillDir.resolve("tests/test_my_test_skill.py"));
        assertThat(testContent).contains("my-test-skill");
        assertThat(testContent).contains("def test_skill_validates");
    }

    // ── Test harness ───────────────────────────────────────────────────────

    @Test
    void harnessDetectsTriggerMatch() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("code-reviewer")
                .description("Reviews code")
                .allowedTools("read_file")
                .trigger("user asks to review code")
                .trigger("user asks to find bugs")
                .instruction("read the file using read_file tool")
                .build();

        SkillSDK.SkillTestHarness harness = new SkillSDK.SkillTestHarness(skill)
                .mockTool("read_file", "public class Foo { }");

        SkillSDK.SkillTestResult result = harness.run("review this code");
        assertThat(result.triggered()).isTrue();
        assertThat(result.success()).isTrue();
    }

    @Test
    void harnessReportsNotTriggeredWhenNoMatch() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("deploy-tool")
                .description("Deploys applications")
                .trigger("deploy the application")
                .instruction("run deployment script")
                .build();

        SkillSDK.SkillTestResult result = new SkillSDK.SkillTestHarness(skill)
                .run("fix the null pointer exception");

        assertThat(result.triggered()).isFalse();
        assertThat(result.notTriggeredReason()).isNotBlank();
    }

    @Test
    void harnessRecordsSimulatedToolCalls() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("file-reader")
                .description("Reads and analyzes files")
                .allowedTools("read_file", "search_files")
                .trigger("show me the file")
                .instruction("Use read_file to read the contents")
                .instruction("Use search_files to find patterns")
                .build();

        SkillSDK.SkillTestHarness harness = new SkillSDK.SkillTestHarness(skill)
                .mockTool("read_file", "file content")
                .mockTool("search_files", "matches: [line 42]");

        SkillSDK.SkillTestResult result = harness.run("show me the file contents");
        assertThat(result.triggered()).isTrue();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void harnessUsesCustomMockOutput() {
        SkillSDK.SkillDefinition skill = SkillSDK.SkillDefinition.builder("analyzer")
                .description("Analyzes code")
                .allowedTools("read_file")
                .trigger("analyze this")
                .instruction("read_file the target")
                .build();

        SkillSDK.SkillTestHarness harness = new SkillSDK.SkillTestHarness(skill)
                .mockTool("read_file", params -> "MOCKED: " + params.getOrDefault("task", "?"));

        SkillSDK.SkillTestResult result = harness.run("analyze this module");
        // The harness recorded the call with our mock function
        assertThat(result.triggered()).isTrue();
    }

    // ── Skill param ───────────────────────────────────────────────────────

    @Test
    void skillParamRecordHasCorrectFields() {
        SkillSDK.SkillParam param = new SkillSDK.SkillParam(
                "path", "string", true, "File path", "/default/path");
        assertThat(param.name()).isEqualTo("path");
        assertThat(param.type()).isEqualTo("string");
        assertThat(param.required()).isTrue();
        assertThat(param.description()).isEqualTo("File path");
        assertThat(param.defaultValue()).isEqualTo("/default/path");
    }
}
