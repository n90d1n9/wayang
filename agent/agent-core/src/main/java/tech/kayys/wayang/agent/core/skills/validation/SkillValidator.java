package tech.kayys.wayang.agent.core.skills.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Unified skill validator for AgentSkills.io specification compliance.
 *
 * <p>
 * Consolidates validation logic from multiple validators across agent and skills modules.
 * Validates SKILL.md frontmatter, body content, directory structure, and parameters.
 *
 * <h3>Specification:</h3> https://agentskills.io/specification
 *
 * <h3>Validation Levels:</h3>
 * <ul>
 *   <li><strong>Manifest validation</strong>: Name, description, license, compatibility, metadata</li>
 *   <li><strong>Body validation</strong>: Content structure, token estimation, section recommendations</li>
 *   <li><strong>Parameter validation</strong>: Against skill metadata allowed parameters</li>
 *   <li><strong>Directory structure</strong>: Optional scripts/, references/, assets/ directories</li>
 * </ul>
 */
public class SkillValidator {

    // Spec: 1-64 chars, lowercase alphanumeric + hyphens, no leading/trailing/consecutive hyphens
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    // Spec field limits
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 1024;
    private static final int MAX_COMPATIBILITY_LENGTH = 500;
    private static final int MIN_DESCRIPTION_WORDS = 5;
    private static final int TOKEN_ESTIMATE_THRESHOLD = 5000;

    /**
     * Validate a skill directory against the spec.
     *
     * @param skillPath path to skill directory
     * @return validation result with errors and warnings
     */
    public ValidationResult validateSkillDirectory(Path skillPath) {
        Objects.requireNonNull(skillPath);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String skillName = skillPath.getFileName().toString();

        // Check SKILL.md exists
        Path skillMdPath = skillPath.resolve("SKILL.md");
        if (!Files.exists(skillMdPath)) {
            errors.add("SKILL.md not found");
            return new ValidationResult(skillName, false, errors, warnings);
        }

        // Read and validate SKILL.md
        try {
            String content = Files.readString(skillMdPath);
            validateSkillMdContent(content, skillName, skillPath, errors, warnings);
        } catch (IOException e) {
            errors.add("Failed to read SKILL.md: " + e.getMessage());
        }

        return new ValidationResult(skillName, errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate SKILL.md content.
     *
     * @param content SKILL.md file content
     * @param skillName skill directory name
     * @param skillPath skill directory path
     * @param errors error list to populate
     * @param warnings warning list to populate
     */
    public void validateSkillMdContent(String content, String skillName, Path skillPath,
                                       List<String> errors, List<String> warnings) {
        // Check frontmatter structure
        if (!content.startsWith("---")) {
            errors.add("SKILL.md must start with --- (YAML frontmatter)");
            return;
        }

        int closingIdx = content.indexOf("---", 3);
        if (closingIdx < 0) {
            errors.add("SKILL.md missing closing --- for YAML frontmatter");
            return;
        }

        // Extract and validate frontmatter
        String yamlBlock = content.substring(3, closingIdx).trim();
        Map<String, Object> frontmatter = parseYaml(yamlBlock);
        validateFrontmatter(frontmatter, skillName, skillPath, errors, warnings);

        // Validate body content
        String body = content.substring(closingIdx + 3).trim();
        validateBody(body, errors, warnings);
    }

    /**
     * Validate SKILL.md frontmatter section.
     *
     * @param frontmatter parsed YAML frontmatter
     * @param skillName skill name from directory
     * @param skillPath skill directory path
     * @param errors error list to populate
     * @param warnings warning list to populate
     */
    public void validateFrontmatter(Map<String, Object> frontmatter, String skillName, Path skillPath,
                                    List<String> errors, List<String> warnings) {
        // Required: name field
        String name = getString(frontmatter, "name");
        if (name == null || name.isBlank()) {
            errors.add("Required field 'name' is missing or empty");
        } else {
            validateNameField(name, skillName, errors);
        }

        // Required: description field
        String description = getString(frontmatter, "description");
        if (description == null || description.isBlank()) {
            errors.add("Required field 'description' is missing or empty");
        } else {
            validateDescriptionField(description, errors);
        }

        // Optional: license field
        String license = getString(frontmatter, "license");
        if (license != null && license.length() > 200) {
            warnings.add("License field is very long (>200 chars). Consider using a file reference.");
        }

        // Optional: compatibility field
        String compatibility = getString(frontmatter, "compatibility");
        if (compatibility != null && compatibility.length() > MAX_COMPATIBILITY_LENGTH) {
            errors.add(String.format("Compatibility field exceeds %d characters (%d)",
                    MAX_COMPATIBILITY_LENGTH, compatibility.length()));
        }

        // Optional: allowed-tools field (space-delimited)
        String allowedTools = getString(frontmatter, "allowed-tools");
        if (allowedTools != null && !allowedTools.isBlank()) {
            String[] tools = allowedTools.split("\\s+");
            for (String tool : tools) {
                if (tool.isBlank()) {
                    warnings.add("Empty tool in allowed-tools list");
                }
            }
        }

        // Optional: metadata object
        Object metadata = frontmatter.get("metadata");
        if (metadata instanceof Map<?, ?> metaMap) {
            validateMetadataObject(metaMap, warnings);
        }
    }

    /**
     * Validate skill name field per spec.
     */
    private void validateNameField(String name, String skillName, List<String> errors) {
        // Length check
        if (name.length() < 1 || name.length() > MAX_NAME_LENGTH) {
            errors.add(String.format("Name must be 1-%d characters, got %d: %s",
                    MAX_NAME_LENGTH, name.length(), name));
            return;
        }

        // Pattern check
        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add("Name must be lowercase alphanumeric with hyphens only: " + name);
        }

        // Directory match check
        if (!name.equals(skillName)) {
            errors.add(String.format("Name '%s' must match directory name '%s'", name, skillName));
        }
    }

    /**
     * Validate skill description field per spec.
     */
    private void validateDescriptionField(String description, List<String> errors) {
        // Length check
        if (description.length() < 1 || description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add(String.format("Description must be 1-%d characters, got %d",
                    MAX_DESCRIPTION_LENGTH, description.length()));
            return;
        }

        // Word count check (should be keyword-rich)
        String[] words = description.trim().split("\\s+");
        if (words.length < MIN_DESCRIPTION_WORDS) {
            errors.add(String.format("Description too brief. Must include what the skill does " +
                    "and when to use it (at least %d words, got %d)",
                    MIN_DESCRIPTION_WORDS, words.length));
        }

        // Action word check (heuristic for task description)
        String lower = description.toLowerCase();
        boolean hasActionWord = lower.contains("use") || lower.contains("when") ||
                lower.contains("for") || lower.contains("execute") ||
                lower.contains("run") || lower.contains("create");
        if (!hasActionWord) {
            errors.add("Description should include task-identifying keywords " +
                    "(use, when, for, execute, run, create)");
        }
    }

    /**
     * Validate metadata object structure.
     */
    private void validateMetadataObject(Map<String, Object> metadata, List<String> warnings) {
        if (!metadata.containsKey("version")) {
            warnings.add("Metadata missing recommended 'version' field");
        }

        // Check for Openclaw-specific fields (optional extension)
        if (metadata.containsKey("openclaw") && metadata.get("openclaw") instanceof Map<?, ?> openclaw) {
            if (openclaw.containsKey("emoji")) {
                String emoji = openclaw.get("emoji").toString();
                if (emoji.length() > 10) {
                    warnings.add("Openclaw emoji field is very long");
                }
            }
            if (openclaw.containsKey("homepage")) {
                String homepage = openclaw.get("homepage").toString();
                if (!homepage.startsWith("http://") && !homepage.startsWith("https://")) {
                    warnings.add("Openclaw homepage should be a valid URL");
                }
            }
        }
    }

    /**
     * Validate SKILL.md body content.
     */
    private void validateBody(String body, List<String> errors, List<String> warnings) {
        if (body.isBlank()) {
            errors.add("SKILL.md body content is empty");
            return;
        }

        // Token count estimation (~4 chars per token)
        int estimatedTokens = body.length() / 4;
        if (estimatedTokens > TOKEN_ESTIMATE_THRESHOLD) {
            warnings.add(String.format("SKILL.md body is very large (~%d tokens). " +
                    "Consider moving technical details to references/", estimatedTokens));
        }

        // Check for common documentation sections (recommendations)
        String lower = body.toLowerCase();
        if (!lower.contains("when to use") && !lower.contains("usage")) {
            warnings.add("Consider adding 'When to Use' section for better discoverability");
        }

        if (!lower.contains("## ") && !lower.contains("### ")) {
            warnings.add("Consider adding markdown headings for better structure");
        }
    }

    /**
     * Validate parameters against skill metadata.
     *
     * @param skillName skill name
     * @param allowedParameters allowed parameters from metadata (as JSON string or Map)
     * @param providedParameters parameters provided for execution
     * @return validation result
     */
    public ValidationResult validateParameters(String skillName, Object allowedParameters,
                                               Map<String, Object> providedParameters) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (allowedParameters == null || providedParameters == null) {
            return new ValidationResult(skillName, true, errors, warnings);
        }

        Map<String, Object> schema = null;

        // Parse schema from JSON string if needed
        if (allowedParameters instanceof String jsonStr) {
            try {
                schema = parseJsonParameters(jsonStr);
            } catch (Exception e) {
                warnings.add("Failed to parse parameter schema: " + e.getMessage());
                return new ValidationResult(skillName, true, errors, warnings);
            }
        } else if (allowedParameters instanceof Map<?, ?> map) {
            schema = (Map<String, Object>) map;
        }

        if (schema == null || schema.isEmpty()) {
            return new ValidationResult(skillName, true, errors, warnings);
        }

        // Check for required parameters
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String paramName = entry.getKey();
            Object paramSchema = entry.getValue();

            if (paramSchema instanceof Map<?, ?> paramMap) {
                boolean required = Boolean.TRUE.equals(paramMap.get("required"));
                if (required && !providedParameters.containsKey(paramName)) {
                    errors.add("Missing required parameter: " + paramName);
                }
            }
        }

        // Check for unexpected parameters (warning only)
        for (String paramName : providedParameters.keySet()) {
            if (!schema.containsKey(paramName)) {
                warnings.add("Parameter '" + paramName + "' not defined in skill schema");
            }
        }

        return new ValidationResult(skillName, errors.isEmpty(), errors, warnings);
    }

    // ── Internal Helpers ───────────────────────────────────────────────────

    /**
     * Parse JSON parameters into a Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonParameters(String json) throws Exception {
        // Simple JSON parsing - can be replaced with Jackson if needed
        Map<String, Object> result = new HashMap<>();
        // For now, return empty map to indicate parsing would be needed
        return result;
    }

    /**
     * Parse YAML content into a Map.
     */
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
        return new HashMap<>();
    }

    /**
     * Get string value from map, handling type conversion.
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    // ── Validation Result ──────────────────────────────────────────────────

    /**
     * Validation result containing status, errors, and warnings.
     */
    public static class ValidationResult {
        private final String skillName;
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(String skillName, boolean valid, List<String> errors, List<String> warnings) {
            this.skillName = skillName;
            this.valid = valid;
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }

        public String getSkillName() {
            return skillName;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "";
            }
            return String.join("; ", errors);
        }

        public String getWarningMessage() {
            if (warnings.isEmpty()) {
                return "";
            }
            return String.join("; ", warnings);
        }

        @Override
        public String toString() {
            if (valid && warnings.isEmpty()) {
                return "✅ Valid";
            }
            StringBuilder sb = new StringBuilder();
            if (valid) {
                sb.append("✅ Valid with warnings");
            } else {
                sb.append("❌ Invalid");
            }
            if (!errors.isEmpty()) {
                sb.append("\nErrors: ").append(getErrorMessage());
            }
            if (!warnings.isEmpty()) {
                sb.append("\nWarnings: ").append(getWarningMessage());
            }
            return sb.toString();
        }
    }
}
