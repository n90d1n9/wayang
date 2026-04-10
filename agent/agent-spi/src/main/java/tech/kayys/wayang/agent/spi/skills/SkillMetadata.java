package tech.kayys.wayang.agent.spi.skills;

import java.util.Map;

/**
 * AgentSkills.io spec-compliant metadata for skill discovery and documentation.
 * Corresponds to SKILL.md frontmatter fields.
 *
 * @param name Skill identifier (lowercase, alphanumeric + hyphens, 1-64 chars, matches directory name)
 * @param description What the skill does and when to use it (1-1024 chars)
 * @param license Optional license name or bundled file reference
 * @param compatibility Optional environment requirements (max 500 chars)
 * @param allowedTools Optional space-delimited list of pre-approved tools
 * @param customMetadata Optional arbitrary key-value properties for extensions
 */
public record SkillMetadata(
        String name,
        String description,
        String license,
        String compatibility,
        String allowedTools,
        Map<String, String> customMetadata) {

    public SkillMetadata {
        customMetadata = customMetadata != null ? Map.copyOf(customMetadata) : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String license;
        private String compatibility;
        private String allowedTools;
        private Map<String, String> customMetadata = Map.of();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder license(String license) {
            this.license = license;
            return this;
        }

        public Builder compatibility(String compatibility) {
            this.compatibility = compatibility;
            return this;
        }

        public Builder allowedTools(String allowedTools) {
            this.allowedTools = allowedTools;
            return this;
        }

        public Builder customMetadata(Map<String, String> customMetadata) {
            this.customMetadata = customMetadata;
            return this;
        }

        public SkillMetadata build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Skill name is required");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Skill description is required");
            }
            return new SkillMetadata(name, description, license, compatibility, allowedTools, customMetadata);
        }
    }
}
