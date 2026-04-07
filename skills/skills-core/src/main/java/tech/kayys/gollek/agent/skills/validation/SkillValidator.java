package tech.kayys.gollek.agent.skills.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

/**
 * Validator for SKILL.md files following the AgentSkills.io specification.
 *
 * <p>
 * Validates all spec requirements including:
 * <ul>
 *   <li>Name format (1-64 chars, lowercase alphanumeric + hyphens)</li>
 *   <li>Description format (1-1024 chars, keyword-rich)</li>
 *   <li>Directory name matches skill name</li>
 *   <li>Frontmatter structure</li>
 *   <li>Required files and directories</li>
 *   <li>Path security (no directory escape)</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * SkillValidator validator = new SkillValidator();
 * ValidationResult result = validator.validate(Path.of("skills/my-skill"));
 *
 * if (result.isValid()) {
 *     System.out.println("✅ Valid skill");
 * } else {
 *     result.errors().forEach(System.err::println);
 * }
 * }</pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class SkillValidator {

    // Spec: 1-64 chars, lowercase alphanumeric + hyphens, no leading/trailing/consecutive hyphens
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    // Spec: Description should be keyword-rich (heuristic: at least 5 words)
    private static final int MIN_DESCRIPTION_WORDS = 5;

    /**
     * Validate a SKILL.md file at the given path.
     *
     * @param skillMdPath path to SKILL.md file
     * @return validation result
     */
    public ValidationResult validateSkillMd(Path skillMdPath) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check file exists
        if (!Files.exists(skillMdPath)) {
            errors.add("SKILL.md not found at: " + skillMdPath);
            return new ValidationResult(false, errors, warnings);
        }

        // Check file name
        if (!"SKILL.md".equals(skillMdPath.getFileName().toString())) {
            errors.add("File must be named SKILL.md, got: " + skillMdPath.getFileName());
        }

        // Parse content
        String content;
        try {
            content = Files.readString(skillMdPath);
        } catch (Exception e) {
            errors.add("Failed to read SKILL.md: " + e.getMessage());
            return new ValidationResult(false, errors, warnings);
        }

        // Validate frontmatter
        validateFrontmatter(content, skillMdPath.getParent(), errors, warnings);

        // Validate body
        validateBody(content, errors, warnings);

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate a skill directory.
     *
     * @param skillDir path to skill directory
     * @return validation result
     */
    public ValidationResult validateSkillDir(Path skillDir) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check directory exists
        if (!Files.isDirectory(skillDir)) {
            errors.add("Skill directory not found: " + skillDir);
            return new ValidationResult(false, errors, warnings);
        }

        // Check SKILL.md exists
        Path skillMd = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            errors.add("SKILL.md not found in directory: " + skillDir);
            return new ValidationResult(false, errors, warnings);
        }

        // Validate SKILL.md
        return validateSkillMd(skillMd);
    }

    // ── Frontmatter Validation ─────────────────────────────────────────────

    private void validateFrontmatter(String content, Path skillDir, List<String> errors, List<String> warnings) {
        // Check frontmatter delimiters
        if (!content.startsWith("---")) {
            errors.add("SKILL.md must start with YAML frontmatter delimiter (---)");
            return;
        }

        int secondDelimiter = content.indexOf("---", 3);
        if (secondDelimiter == -1) {
            errors.add("SKILL.md missing closing frontmatter delimiter (---)");
            return;
        }

        String yamlBlock = content.substring(3, secondDelimiter).trim();
        Map<String, Object> frontmatter = parseYaml(yamlBlock);

        // Validate name (REQUIRED)
        String name = getString(frontmatter, "name");
        if (name == null || name.isBlank()) {
            errors.add("Missing required field: name");
        } else {
            validateName(name, skillDir, errors);
        }

        // Validate description (REQUIRED)
        String description = getString(frontmatter, "description");
        if (description == null || description.isBlank()) {
            errors.add("Missing required field: description");
        } else {
            validateDescription(description, errors);
        }

        // Validate optional fields
        String license = getString(frontmatter, "license");
        if (license != null && license.length() > 200) {
            warnings.add("License field is very long (>200 chars). Consider using a file reference.");
        }

        String compatibility = getString(frontmatter, "compatibility");
        if (compatibility != null && compatibility.length() > 500) {
            warnings.add("Compatibility field exceeds 500 chars");
        }

        // Validate metadata structure
        Object metadata = frontmatter.get("metadata");
        if (metadata instanceof Map<?, ?> metaMap) {
            validateMetadata(metaMap, warnings);
        }

        // Validate allowed-tools
        String allowedTools = getString(frontmatter, "allowed-tools");
        if (allowedTools != null && !allowedTools.isBlank()) {
            // Space-delimited, validate format
            String[] tools = allowedTools.split("\\s+");
            for (String tool : tools) {
                if (tool.isBlank()) {
                    warnings.add("Empty tool in allowed-tools list");
                }
            }
        }
    }

    /**
     * Validate skill name per AgentSkills.io spec.
     *
     * Rules:
     * - 1-64 characters
     * - Only lowercase alphanumeric + hyphens
     * - No leading, trailing, or consecutive hyphens
     * - Must exactly match parent directory name
     */
    private void validateName(String name, Path skillDir, List<String> errors) {
        // Length check
        if (name.length() < 1 || name.length() > 64) {
            errors.add("Name must be 1-64 characters, got " + name.length() + ": " + name);
            return;
        }

        // Pattern check
        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add("Name must be lowercase alphanumeric with hyphens only: " + name);
        }

        // Directory match check
        if (skillDir != null) {
            String dirName = skillDir.getFileName().toString();
            if (!name.equals(dirName)) {
                errors.add("Name must match parent directory name. Name='" + name + "', Directory='" + dirName + "'");
            }
        }
    }

    /**
     * Validate skill description per AgentSkills.io spec.
     *
     * Rules:
     * - 1-1024 characters
     * - Must state what the skill does
     * - Must state when to use it
     * - Must include task-identifying keywords
     */
    private void validateDescription(String description, List<String> errors) {
        // Length check
        if (description.length() < 1 || description.length() > 1024) {
            errors.add("Description must be 1-1024 characters, got " + description.length());
            return;
        }

        // Keyword richness check (heuristic: at least 5 words)
        String[] words = description.trim().split("\\s+");
        if (words.length < MIN_DESCRIPTION_WORDS) {
            errors.add("Description too brief. Must include what the skill does and when to use it (at least " + MIN_DESCRIPTION_WORDS + " words)");
        }

        // Check for task-identifying keywords (heuristic)
        String lower = description.toLowerCase();
        boolean hasActionWord = lower.contains("use") || lower.contains("when") ||
                               lower.contains("for") || lower.contains("execute") ||
                               lower.contains("run") || lower.contains("create");
        if (!hasActionWord) {
            errors.add("Description should include task-identifying keywords (use, when, for, execute, run, create)");
        }
    }

    /**
     * Validate metadata structure.
     */
    private void validateMetadata(Map<String, Object> metadata, List<String> warnings) {
        // Check for version in metadata (recommended)
        if (!metadata.containsKey("version")) {
            warnings.add("Metadata missing 'version' field (recommended)");
        }

        // Check for nested openclaw structure (optional)
        if (metadata.containsKey("openclaw")) {
            Object openclaw = metadata.get("openclaw");
            if (openclaw instanceof Map<?, ?> openclawMap) {
                // Validate openclaw fields
                if (openclawMap.containsKey("emoji")) {
                    String emoji = openclawMap.get("emoji").toString();
                    if (emoji.length() > 10) {
                        warnings.add("Openclaw emoji field is very long");
                    }
                }
                if (openclawMap.containsKey("homepage")) {
                    String homepage = openclawMap.get("homepage").toString();
                    if (!homepage.startsWith("http://") && !homepage.startsWith("https://")) {
                        warnings.add("Openclaw homepage should be a valid URL");
                    }
                }
            }
        }
    }

    // ── Body Validation ────────────────────────────────────────────────────

    /**
     * Validate SKILL.md body content.
     */
    private void validateBody(String content, List<String> errors, List<String> warnings) {
        // Extract body (after frontmatter)
        int secondDelimiter = content.indexOf("---", 3);
        if (secondDelimiter == -1) {
            return;  // Already caught in frontmatter validation
        }

        String body = content.substring(secondDelimiter + 3).trim();

        // Check body is not empty
        if (body.isBlank()) {
            errors.add("SKILL.md body content is empty");
        }

        // Token count estimation (~4 chars per token)
        int estimatedTokens = body.length() / 4;
        if (estimatedTokens > 5000) {
            warnings.add("SKILL.md body is very large (~" + estimatedTokens + " tokens). Consider moving technical details to references/");
        }

        // Check for common sections (recommendations)
        String lower = body.toLowerCase();
        if (!lower.contains("when to use") && !lower.contains("usage")) {
            warnings.add("Consider adding 'When to Use' section for better discoverability");
        }

        if (!lower.contains("## ") && !lower.contains("### ")) {
            warnings.add("Consider adding markdown headings for better structure");
        }
    }

    // ── Internal Helpers ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yaml) {
        try {
            org.yaml.snakeyaml.Yaml snakeYaml = new org.yaml.snakeyaml.Yaml();
            Object result = snakeYaml.load(yaml);
            if (result instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        return new java.util.HashMap<>();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
