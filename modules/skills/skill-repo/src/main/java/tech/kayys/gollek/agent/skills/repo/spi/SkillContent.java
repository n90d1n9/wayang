package tech.kayys.gollek.agent.skills.repo.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill content with metadata.
 *
 * @param metadata skill metadata
 * @param content skill implementation content
 * @param manifest skill manifest
 */
public record SkillContent(
    SkillMetadata metadata,
    String content,
    Map<String, Object> manifest
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SkillMetadata metadata;
        private String content;
        private Map<String, Object> manifest;

        public Builder metadata(SkillMetadata metadata) { this.metadata = metadata; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder manifest(Map<String, Object> manifest) { this.manifest = manifest; return this; }
        
        public Builder from(SkillContent other) {
            this.metadata = other.metadata();
            this.content = other.content();
            this.manifest = other.manifest();
            return this;
        }

        public SkillContent build() {
            return new SkillContent(metadata, content, manifest);
        }
    }
}
