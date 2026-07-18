package tech.kayys.wayang.agent.core.skills;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads and manages skills from the gollek/skills directory.
 * Integrates with the Agent Skills specification: https://agentskills.io/specification
 */
public class SkillsLoader {

    private static final Logger LOGGER = Logger.getLogger(SkillsLoader.class.getName());
    private static final String SKILL_MD = "SKILL.md";

    private final Path skillsDirectory;
    private final Map<String, SkillMetadata> loadedSkills = new HashMap<>();
    private final Yaml yaml = new Yaml();

    public SkillsLoader(Path skillsDirectory) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory);
        if (!Files.isDirectory(skillsDirectory)) {
            throw new IllegalArgumentException("Skills directory does not exist: " + skillsDirectory);
        }
    }

    public SkillsLoader(String skillsDirectoryPath) {
        this(Paths.get(skillsDirectoryPath));
    }

    /**
     * Loads all skills from the skills directory.
     * @return Map of skill name to metadata
     * @throws IOException if skills cannot be read
     */
    public Map<String, SkillMetadata> loadAllSkills() throws IOException {
        loadedSkills.clear();

        try (var stream = Files.list(skillsDirectory)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().equals(".DS_Store"))
                    .forEach(skillPath -> {
                        String skillName = skillPath.getFileName().toString();
                        try {
                            SkillMetadata metadata = loadSkill(skillPath);
                            loadedSkills.put(skillName, metadata);
                            LOGGER.info("Loaded skill: " + skillName);
                        } catch (Exception e) {
                            LOGGER.warning("Failed to load skill " + skillName + ": " + e.getMessage());
                        }
                    });
        }

        LOGGER.info("Loaded " + loadedSkills.size() + " skills");
        return Map.copyOf(loadedSkills);
    }

    /**
     * Loads a single skill by name.
     * @param skillName The skill directory name
     * @return Skill metadata
     * @throws IOException if skill cannot be read
     */
    public SkillMetadata loadSkill(String skillName) throws IOException {
        Path skillPath = skillsDirectory.resolve(skillName);
        if (!Files.isDirectory(skillPath)) {
            throw new IllegalArgumentException("Skill directory not found: " + skillName);
        }
        return loadSkill(skillPath);
    }

    /**
     * Loads a skill from a skill directory path.
     * @param skillPath Path to the skill directory
     * @return Skill metadata
     * @throws IOException if skill cannot be read
     */
    private SkillMetadata loadSkill(Path skillPath) throws IOException {
        Path skillMdPath = skillPath.resolve(SKILL_MD);
        if (!Files.exists(skillMdPath)) {
            throw new IOException("SKILL.md not found in " + skillPath);
        }

        String content = Files.readString(skillMdPath, StandardCharsets.UTF_8);
        return parseSkillMetadata(content, skillPath.getFileName().toString());
    }

    /**
     * Parses SKILL.md content to extract metadata.
     * @param content The SKILL.md file content
     * @param skillName The skill name
     * @return Parsed skill metadata
     */
    private SkillMetadata parseSkillMetadata(String content, String skillName) {
        // Extract YAML frontmatter
        String[] parts = content.split("\n---\n", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid SKILL.md format: missing frontmatter");
        }

        String frontmatterStr = parts[0].replaceFirst("^---\n", "");
        String body = parts.length > 1 ? parts[1] : "";

        @SuppressWarnings("unchecked")
        Map<String, Object> frontmatter = (Map<String, Object>) yaml.load(frontmatterStr);

        if (frontmatter == null) {
            frontmatter = new HashMap<>();
        }

        return new SkillMetadata(
                skillName,
                getString(frontmatter, "name", skillName),
                getString(frontmatter, "description", ""),
                getString(frontmatter, "license", ""),
                getString(frontmatter, "compatibility", ""),
                getMap(frontmatter, "metadata", Map.of()),
                getString(frontmatter, "allowed-tools", ""),
                body.trim()
        );
    }

    /**
     * Gets a string value from a map, with a default.
     */
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * Gets a map value from a map, with a default.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key, Map<String, Object> defaultValue) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : defaultValue;
    }

    /**
     * Gets a loaded skill by name.
     * @param skillName The skill name
     * @return Optional containing the skill metadata if found
     */
    public Optional<SkillMetadata> getSkill(String skillName) {
        return Optional.ofNullable(loadedSkills.get(skillName));
    }

    /**
     * Gets all loaded skills.
     * @return Map of skill name to metadata
     */
    public Map<String, SkillMetadata> getLoadedSkills() {
        return Map.copyOf(loadedSkills);
    }

    /**
     * Gets a list of all loaded skill names.
     * @return List of skill names
     */
    public List<String> getSkillNames() {
        return new ArrayList<>(loadedSkills.keySet());
    }

    /**
     * Validates a skill against the specification.
     * @param skillName The skill name to validate
     * @return Validation result
     */
    public ValidationResult validateSkill(String skillName) {
        Optional<SkillMetadata> skill = getSkill(skillName);
        if (skill.isEmpty()) {
            return ValidationResult.error("Skill not found: " + skillName);
        }

        SkillMetadata metadata = skill.get();
        List<String> errors = new ArrayList<>();

        // Validate name
        if (!metadata.getName().matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")) {
            errors.add("Invalid skill name format");
        }

        // Validate description
        if (metadata.getDescription().length() < 1 || metadata.getDescription().length() > 1024) {
            errors.add("Description must be 1-1024 characters");
        }

        // Validate compatibility
        if (!metadata.getCompatibility().isEmpty() && 
            metadata.getCompatibility().length() > 500) {
            errors.add("Compatibility must be 1-500 characters");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.errors(errors);
    }

    /**
     * Metadata for a skill.
     */
    public static final class SkillMetadata {
        private final String directoryName;
        private final String name;
        private final String description;
        private final String license;
        private final String compatibility;
        private final Map<String, Object> metadata;
        private final String allowedTools;
        private final String body;

        public SkillMetadata(String directoryName, String name, String description, String license,
                           String compatibility, Map<String, Object> metadata, String allowedTools, String body) {
            this.directoryName = Objects.requireNonNull(directoryName);
            this.name = Objects.requireNonNull(name);
            this.description = Objects.requireNonNull(description);
            this.license = Objects.requireNonNull(license);
            this.compatibility = Objects.requireNonNull(compatibility);
            this.metadata = Map.copyOf(Objects.requireNonNull(metadata));
            this.allowedTools = Objects.requireNonNull(allowedTools);
            this.body = Objects.requireNonNull(body);
        }

        public String getDirectoryName() { return directoryName; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getLicense() { return license; }
        public String getCompatibility() { return compatibility; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getAllowedTools() { return allowedTools; }
        public String getBody() { return body; }

        @Override
        public String toString() {
            return "SkillMetadata{" +
                    "name='" + name + '\'' +
                    ", description='" + description.substring(0, Math.min(50, description.length())) + "...'" +
                    '}';
        }
    }

    /**
     * Validation result.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = List.copyOf(errors);
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, List.of(message));
        }

        public static ValidationResult errors(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors));
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + String.join(", ", errors);
        }
    }
}
