package tech.kayys.gollek.agent.skills.repo.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill metadata stored in repository.
 *
 * @param id unique skill identifier
 * @param name human-readable name
 * @param version skill version (semver)
 * @param description skill description
 * @param category skill category
 * @param author skill author
 * @param tags searchable tags
 * @param contentPath path to skill content (file path or DB reference)
 * @param manifestPath path to manifest file
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param enabled whether skill is enabled
 * @param checksum content checksum for validation
 */
public record SkillMetadata(
    String id,
    String name,
    String version,
    String description,
    String category,
    String author,
    List<String> tags,
    String contentPath,
    String manifestPath,
    Instant createdAt,
    Instant updatedAt,
    boolean enabled,
    String checksum
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String version = "1.0.0";
        private String description;
        private String category = "GENERAL";
        private String author = "unknown";
        private List<String> tags = List.of();
        private String contentPath;
        private String manifestPath;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private boolean enabled = true;
        private String checksum;

        public Builder from(SkillMetadata existing) {
            this.id = existing.id;
            this.name = existing.name;
            this.version = existing.version;
            this.description = existing.description;
            this.category = existing.category;
            this.author = existing.author;
            this.tags = existing.tags;
            this.contentPath = existing.contentPath;
            this.manifestPath = existing.manifestPath;
            this.createdAt = existing.createdAt;
            this.updatedAt = existing.updatedAt;
            this.enabled = existing.enabled;
            this.checksum = existing.checksum;
            return this;
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder contentPath(String path) { this.contentPath = path; return this; }
        public Builder manifestPath(String path) { this.manifestPath = path; return this; }
        public Builder createdAt(Instant instant) { this.createdAt = instant; return this; }
        public Builder updatedAt(Instant instant) { this.updatedAt = instant; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }

        public SkillMetadata build() {
            return new SkillMetadata(
                id, name, version, description, category, author, tags,
                contentPath, manifestPath, createdAt, updatedAt, enabled, checksum
            );
        }
    }
}
