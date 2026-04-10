package tech.kayys.wayang.agent.core.skills.manifest;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unified parser for SKILL.md files following the Agent Skills specification.
 *
 * <p>
 * Splits YAML frontmatter (between {@code ---} delimiters) from the markdown body
 * and constructs a {@link SkillManifest}. Handles both simple and extended metadata.
 *
 * <h3>Supported Format:</h3>
 *
 * <pre>
 * ---
 * name: skill-name
 * description: "What the skill does and when to use it."
 * license: MIT
 * compatibility: "Environment requirements"
 * allowed-tools: "tool1 tool2"
 * metadata:
 *   author: Author Name
 *   version: "1.0.0"
 * ---
 *
 * # Skill Body Content
 * Markdown documentation...
 * </pre>
 */
public final class SkillManifestParser {

    private static final String FRONTMATTER_DELIMITER = "---";
    private static final Yaml YAML = new Yaml();

    private SkillManifestParser() {
    }

    /**
     * Parse a SKILL.md file from the given path.
     *
     * @param skillMdPath path to SKILL.md
     * @return parsed manifest
     * @throws IOException if file cannot be read
     * @throws SkillParseException if format is invalid
     */
    public static SkillManifest parse(Path skillMdPath) throws IOException {
        String content = Files.readString(skillMdPath);
        Path skillDir = skillMdPath.getParent();
        return parse(content, skillDir);
    }

    /**
     * Parse SKILL.md content with a known source directory.
     *
     * @param content  raw SKILL.md content
     * @param skillDir directory containing the SKILL.md (for resolving references)
     * @return parsed manifest
     */
    public static SkillManifest parse(String content, Path skillDir) {
        if (content == null || content.isBlank()) {
            throw new SkillParseException("SKILL.md content is empty");
        }

        String[] parts = splitFrontmatter(content);
        String yamlBlock = parts[0];
        String bodyContent = parts[1];

        Map<String, Object> frontmatter = parseFrontmatter(yamlBlock);

        SkillManifest.Builder builder = SkillManifest.builder()
                .name(getString(frontmatter, "name"))
                .description(getString(frontmatter, "description"))
                .license(getString(frontmatter, "license"))
                .compatibility(getString(frontmatter, "compatibility"))
                .userInvocable(getBoolean(frontmatter, "user-invocable", false))
                .bodyContent(bodyContent.strip())
                .sourceDirectory(skillDir);

        // Parse allowed-tools (space-delimited string, per AgentSkills.io spec)
        String allowedTools = getString(frontmatter, "allowed-tools");
        if (allowedTools != null && !allowedTools.isBlank()) {
            builder.allowedToolsString(allowedTools);
            builder.allowedTools(
                    Arrays.stream(allowedTools.split("\\s+"))
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList()));
        }

        // Parse metadata sub-object (extended fields)
        Object metadataObj = frontmatter.get("metadata");
        if (metadataObj instanceof Map<?, ?> metadata) {
            builder.author(getNestedString(metadata, "author"));
            builder.metadataVersion(getNestedString(metadata, "version"));

            // Parse version from metadata if present
            String metaVersion = getNestedString(metadata, "version");
            if (metaVersion != null) {
                builder.version(metaVersion);
            }

            // Parse openclaw sub-object (extended metadata for tooling)
            Object openclawObj = metadata.get("openclaw");
            if (openclawObj instanceof Map<?, ?> openclaw) {
                builder.emoji(getNestedString(openclaw, "emoji"));
                builder.homepage(getNestedString(openclaw, "homepage"));

                // Parse requires.bins
                Object requiresObj = openclaw.get("requires");
                if (requiresObj instanceof Map<?, ?> requires) {
                    Object binsObj = requires.get("bins");
                    if (binsObj instanceof List<?> bins) {
                        builder.requiredBins(
                                bins.stream()
                                        .map(Object::toString)
                                        .collect(Collectors.toList()));
                    }
                }
            }
        }

        // Load references if references/ directory exists
        if (skillDir != null) {
            Map<String, String> references = loadReferences(skillDir);
            if (!references.isEmpty()) {
                builder.references(references);
            }
        }

        return builder.build();
    }

    /**
     * Scan a directory for SKILL.md files.
     * Expects structure: {@code baseDir/skills/skill-name/SKILL.md} or {@code baseDir/skill-name/SKILL.md}.
     *
     * @param baseDir root directory to scan
     * @return list of parsed manifests
     */
    public static List<SkillManifest> scanDirectory(Path baseDir) {
        List<SkillManifest> manifests = new ArrayList<>();

        // Try skills/ subdirectory first (AgentSkills.io convention)
        Path skillsDir = baseDir.resolve("skills");
        Path scanDir = Files.isDirectory(skillsDir) ? skillsDir : baseDir;

        try (Stream<Path> dirs = Files.list(scanDir)) {
            dirs.filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path skillMd = dir.resolve("SKILL.md");
                        if (Files.isRegularFile(skillMd)) {
                            try {
                                manifests.add(parse(skillMd));
                            } catch (Exception e) {
                                // Log and continue — one bad skill shouldn't break discovery
                                System.err.println("Warning: Failed to parse " + skillMd + ": " + e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            // Directory doesn't exist or isn't readable
        }

        return manifests;
    }

    /**
     * Validate a SKILL.md against AgentSkills.io specification constraints.
     *
     * @param manifest the manifest to validate
     * @return validation result with any errors
     */
    public static ValidationResult validate(SkillManifest manifest) {
        List<String> errors = new ArrayList<>();

        // Name validation (per spec)
        if (manifest.getName() == null || manifest.getName().isBlank()) {
            errors.add("name is required");
        } else if (manifest.getName().length() > 64) {
            errors.add("name must be 1-64 characters");
        } else if (!manifest.getName().matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {
            errors.add("name must contain only lowercase letters, numbers, and hyphens");
        }

        // Description validation (per spec)
        if (manifest.getDescription() == null || manifest.getDescription().isBlank()) {
            errors.add("description is required");
        } else if (manifest.getDescription().length() > 1024) {
            errors.add("description must be 1-1024 characters");
        }

        // Compatibility validation (per spec)
        if (manifest.getCompatibility() != null && manifest.getCompatibility().length() > 500) {
            errors.add("compatibility must be max 500 characters");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ── Internal ──────────────────────────────────────────────────

    private static String[] splitFrontmatter(String content) {
        String trimmed = content.strip();
        if (!trimmed.startsWith(FRONTMATTER_DELIMITER)) {
            throw new SkillParseException(
                    "SKILL.md must start with YAML frontmatter (---). Got: " +
                            trimmed.substring(0, Math.min(50, trimmed.length())));
        }

        int secondDelimiter = trimmed.indexOf(FRONTMATTER_DELIMITER, FRONTMATTER_DELIMITER.length());
        if (secondDelimiter < 0) {
            throw new SkillParseException("SKILL.md missing closing frontmatter delimiter (---)");
        }

        String yaml = trimmed.substring(FRONTMATTER_DELIMITER.length(), secondDelimiter).strip();
        String body = trimmed.substring(secondDelimiter + FRONTMATTER_DELIMITER.length()).strip();

        return new String[] { yaml, body };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseFrontmatter(String yamlBlock) {
        try {
            Object parsed = YAML.load(yamlBlock);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            throw new SkillParseException("YAML frontmatter must be a mapping, got: " +
                    (parsed != null ? parsed.getClass().getSimpleName() : "null"));
        } catch (Exception e) {
            if (e instanceof SkillParseException)
                throw e;
            throw new SkillParseException("Failed to parse YAML frontmatter: " + e.getMessage(), e);
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean b)
            return b;
        if (val != null)
            return Boolean.parseBoolean(val.toString());
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static String getNestedString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static Map<String, String> loadReferences(Path skillDir) {
        Path referencesDir = skillDir.resolve("references");
        Map<String, String> refs = new LinkedHashMap<>();

        if (!Files.isDirectory(referencesDir))
            return refs;

        try (Stream<Path> files = Files.list(referencesDir)) {
            files.filter(f -> f.toString().endsWith(".md") && Files.isRegularFile(f))
                    .forEach(f -> {
                        try {
                            String refName = f.getFileName().toString().replace(".md", "");
                            refs.put(refName, Files.readString(f));
                        } catch (IOException e) {
                            // Skip unreadable reference
                        }
                    });
        } catch (IOException e) {
            // References dir not readable
        }

        return refs;
    }

    // ── Validation Result ─────────────────────────────────────────

    public static final class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    // ── Exceptions ────────────────────────────────────────────────

    public static class SkillParseException extends RuntimeException {
        public SkillParseException(String msg) {
            super(msg);
        }

        public SkillParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
