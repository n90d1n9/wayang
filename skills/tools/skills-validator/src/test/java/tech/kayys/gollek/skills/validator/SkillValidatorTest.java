package tech.kayys.gollek.skills.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillValidator.
 */
public class SkillValidatorTest {

    private SkillValidator validator;

    @BeforeEach
    void setup() {
        validator = new SkillValidator();
    }

    @Test
    void testValidSkill(@TempDir Path tempDir) throws IOException {
        String skillName = "test-skill";
        Path skillDir = tempDir.resolve(skillName);
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: test-skill\n" +
                "description: A test skill for validation\n" +
                "---\n" +
                "\n" +
                "# Test Skill\n" +
                "\n" +
                "This is a test skill.\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertTrue(result.isValid());
        assertEquals(skillName, result.getSkillName());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testMissingSkillMd(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("missing-skill");
        Files.createDirectory(skillDir);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("SKILL.md not found"));
    }

    @Test
    void testMissingNameField(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("no-name");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "description: Missing name field\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'name'")));
    }

    @Test
    void testMissingDescriptionField(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("no-desc");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: no-desc\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'description'")));
    }

    @Test
    void testInvalidNameFormat(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("invalid-name");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: UPPERCASE\n" +
                "description: Test\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
    }

    @Test
    void testNameMismatch(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("skill-a");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: skill-b\n" +
                "description: Name mismatch test\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("must match")));
    }

    @Test
    void testDescriptionTooLong(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("long-desc");
        Files.createDirectory(skillDir);

        String longDesc = "a".repeat(1025);
        String skillMd = "---\n" +
                "name: long-desc\n" +
                "description: " + longDesc + "\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
    }

    @Test
    void testNameWithConsecutiveHyphens(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("skill--bad");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: skill--bad\n" +
                "description: Consecutive hyphens test\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("consecutive")));
    }

    @Test
    void testValidMetadata(@TempDir Path tempDir) throws IOException {
        Path skillDir = tempDir.resolve("skill-meta");
        Files.createDirectory(skillDir);

        String skillMd = "---\n" +
                "name: skill-meta\n" +
                "description: Test with metadata\n" +
                "metadata:\n" +
                "  author: test\n" +
                "  version: \"1.0\"\n" +
                "---\n" +
                "\n" +
                "Content\n";

        Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

        ValidationResult result = validator.validate(skillDir);

        assertTrue(result.isValid());
    }
}
