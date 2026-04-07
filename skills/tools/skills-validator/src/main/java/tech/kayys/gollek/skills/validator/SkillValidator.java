package tech.kayys.gollek.skills.validator;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates individual skills against the Agent Skills specification.
 * Specification: https://agentskills.io/specification
 */
public final class SkillValidator {

    private static final String SKILL_MD = "SKILL.md";
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 1024;
    private static final int MAX_COMPATIBILITY_LENGTH = 500;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

    private final Yaml yaml = new Yaml();

    /**
     * Validates a skill directory.
     *
     * @param skillPath Path to the skill directory
     * @return ValidationResult containing any errors and warnings
     */
    public ValidationResult validate(Path skillPath) {
        Objects.requireNonNull(skillPath);

        String skillName = skillPath.getFileName().toString();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check SKILL.md exists
        Path skillMdPath = skillPath.resolve(SKILL_MD);
        if (!Files.exists(skillMdPath)) {
            errors.add("SKILL.md not found");
            return ValidationResult.invalid(skillName, errors);
        }

        // Read and parse SKILL.md
        try {
            String content = Files.readString(skillMdPath, StandardCharsets.UTF_8);

            if (!content.startsWith("---")) {
                errors.add("SKILL.md must start with --- (YAML frontmatter)");
                return ValidationResult.invalid(skillName, errors);
            }

            // Extract frontmatter
            Map<String, Object> frontmatter = extractFrontmatter(content, skillName, errors);
            if (frontmatter == null) {
                return ValidationResult.invalid(skillName, errors);
            }

            // Validate required fields
            validateNameField(frontmatter, skillName, errors);
            validateDescriptionField(frontmatter, skillName, errors);

            // Validate optional fields
            if (frontmatter.containsKey("compatibility")) {
                validateCompatibilityField(frontmatter, skillName, warnings);
            }

            // Check body content
            validateBodyContent(content, skillName, warnings);

            // Check optional directories
            checkOptionalDirectories(skillPath, skillName);

        } catch (IOException e) {
            errors.add("Failed to read SKILL.md: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            return ValidationResult.invalid(skillName, errors);
        }

        if (!warnings.isEmpty()) {
            return ValidationResult.withWarnings(skillName, warnings);
        }

        return ValidationResult.valid(skillName);
    }

    private Map<String, Object> extractFrontmatter(String content, String skillName, List<String> errors) {
        String[] lines = content.split("\n", -1);

        if (lines.length < 2 || !lines[0].equals("---")) {
            errors.add("No opening --- found for YAML frontmatter");
            return null;
        }

        // Find closing ---
        int closingIdx = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].equals("---")) {
                closingIdx = i;
                break;
            }
        }

        if (closingIdx < 0) {
            errors.add("No closing --- found for YAML frontmatter");
            return null;
        }

        // Extract frontmatter content
        String frontmatterStr = String.join("\n", java.util.Arrays.copyOfRange(lines, 1, closingIdx));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> frontmatter = (Map<String, Object>) yaml.load(frontmatterStr);
            return frontmatter == null ? Map.of() : frontmatter;
        } catch (YAMLException e) {
            errors.add("Invalid YAML frontmatter: " + e.getMessage());
            return null;
        }
    }

    private void validateNameField(Map<String, Object> frontmatter, String skillName, List<String> errors) {
        Object nameObj = frontmatter.get("name");

        if (nameObj == null) {
            errors.add("'name' field is required");
            return;
        }

        String nameValue = String.valueOf(nameObj).trim();

        if (nameValue.isEmpty()) {
            errors.add("'name' field cannot be empty");
            return;
        }

        if (nameValue.length() > MAX_NAME_LENGTH) {
            errors.add(String.format("'name' exceeds %d characters (%d)", MAX_NAME_LENGTH, nameValue.length()));
            return;
        }

        if (!NAME_PATTERN.matcher(nameValue).matches()) {
            errors.add("'name' contains invalid characters. Must be lowercase letters, numbers, and hyphens only");
            return;
        }

        if (nameValue.startsWith("-")) {
            errors.add("'name' cannot start with a hyphen");
            return;
        }

        if (nameValue.endsWith("-")) {
            errors.add("'name' cannot end with a hyphen");
            return;
        }

        if (nameValue.contains("--")) {
            errors.add("'name' cannot contain consecutive hyphens");
            return;
        }

        if (!nameValue.equals(skillName)) {
            errors.add(String.format("'name' (%s) must match directory name (%s)", nameValue, skillName));
        }
    }

    private void validateDescriptionField(Map<String, Object> frontmatter, String skillName, List<String> errors) {
        Object descObj = frontmatter.get("description");

        if (descObj == null) {
            errors.add("'description' field is required");
            return;
        }

        String description = String.valueOf(descObj).trim();
        int length = description.length();

        if (length == 0) {
            errors.add("'description' field cannot be empty");
            return;
        }

        if (length > MAX_DESCRIPTION_LENGTH) {
            errors.add(String.format("'description' exceeds %d characters (%d)", MAX_DESCRIPTION_LENGTH, length));
        }
    }

    private void validateCompatibilityField(Map<String, Object> frontmatter, String skillName, List<String> warnings) {
        Object compatObj = frontmatter.get("compatibility");
        if (compatObj != null) {
            String compat = String.valueOf(compatObj).trim();
            if (compat.length() > MAX_COMPATIBILITY_LENGTH) {
                warnings.add(String.format("'compatibility' exceeds %d characters (%d)",
                        MAX_COMPATIBILITY_LENGTH, compat.length()));
            }
        }
    }

    private void validateBodyContent(String content, String skillName, List<String> warnings) {
        String[] lines = content.split("\n", -1);

        // Find closing ---
        int closingIdx = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].equals("---")) {
                closingIdx = i;
                break;
            }
        }

        if (closingIdx >= 0 && closingIdx < lines.length - 1) {
            int bodyLineCount = lines.length - closingIdx - 1;
            String bodyContent = String.join("\n", java.util.Arrays.copyOfRange(lines, closingIdx + 1, lines.length));
            int nonEmptyLines = (int) java.util.Arrays.stream(bodyContent.split("\n"))
                    .filter(line -> !line.trim().isEmpty())
                    .count();

            if (nonEmptyLines < 2) {
                warnings.add(String.format("Body content is very minimal (%d lines)", bodyLineCount));
            }
        }
    }

    private void checkOptionalDirectories(Path skillPath, String skillName) {
        // Optional directories: scripts/, references/, assets/
        // No validation needed, just informational
    }
}
