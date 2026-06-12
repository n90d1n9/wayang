package tech.kayys.gamelan.skill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class SkillLoaderTest {

    private final SkillLoader loader = new SkillLoader();

    @Test
    void loadsMinimalSkill(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("my-skill");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("SKILL.md"), """
                ---
                name: my-skill
                description: Does something useful.
                ---
                
                # My Skill
                
                Step-by-step instructions here.
                """);

        Skill skill = loader.load(dir);

        assertThat(skill.name()).isEqualTo("my-skill");
        assertThat(skill.description()).isEqualTo("Does something useful.");
        assertThat(skill.instructions()).contains("Step-by-step instructions here.");
        assertThat(skill.license()).isEmpty();
        assertThat(skill.scriptPaths()).isEmpty();
        assertThat(skill.references()).isEmpty();
    }

    @Test
    void loadsFullSkillWithReferences(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("full-skill");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("SKILL.md"), """
                ---
                name: full-skill
                description: Full featured skill.
                license: Apache-2.0
                compatibility: Requires Java 21
                metadata:
                  author: test
                  version: "2.0"
                allowed-tools: read_file write_file
                ---
                Body instructions.
                """);

        Path refs = dir.resolve("references");
        Files.createDirectory(refs);
        Files.writeString(refs.resolve("REFERENCE.md"), "# Reference\nSome ref content.");

        Path scripts = dir.resolve("scripts");
        Files.createDirectory(scripts);
        Files.writeString(scripts.resolve("main.py"), "print('hello')");

        Skill skill = loader.load(dir);

        assertThat(skill.license()).isEqualTo("Apache-2.0");
        assertThat(skill.compatibility()).isEqualTo("Requires Java 21");
        assertThat(skill.metadata()).containsEntry("author", "test");
        assertThat(skill.allowedTools()).containsExactly("read_file", "write_file");
        assertThat(skill.references()).containsKey("REFERENCE.md");
        assertThat(skill.scriptPaths()).containsExactly("main.py");
    }

    @Test
    void handlesNoFrontmatter(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("no-fm");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("SKILL.md"), "Just instructions, no frontmatter.");

        Skill skill = loader.load(dir);

        // Name falls back to directory name
        assertThat(skill.name()).isEqualTo("no-fm");
        assertThat(skill.instructions()).contains("Just instructions");
    }

    @Test
    void handlesCrlfLineEndings(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("crlf-skill");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("SKILL.md"),
                "---\r\nname: crlf-skill\r\ndescription: Test.\r\n---\r\nInstructions.\r\n");

        Skill skill = loader.load(dir);
        assertThat(skill.name()).isEqualTo("crlf-skill");
        assertThat(skill.instructions()).contains("Instructions.");
    }

    @Test
    void throwsForMissingSkillMd(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("empty-skill");
        Files.createDirectory(dir);

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("SKILL.md");
    }

    @Test
    void truncatesLargeReferenceFiles(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("big-ref");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("SKILL.md"), """
                ---
                name: big-ref
                description: Has big refs.
                ---
                Body.
                """);
        Path refs = dir.resolve("references");
        Files.createDirectory(refs);
        // Write 20 KB reference
        Files.writeString(refs.resolve("big.md"), "x".repeat(20_480));

        Skill skill = loader.load(dir);
        String refContent = skill.references().get("big.md");
        assertThat(refContent).contains("truncated");
        assertThat(refContent.length()).isLessThan(12_000);
    }
}
