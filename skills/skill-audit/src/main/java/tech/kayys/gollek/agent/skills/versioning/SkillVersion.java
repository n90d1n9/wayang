package tech.kayys.gollek.agent.skills.versioning;

import java.time.Instant;

/**
 * Skill version information.
 *
 * @param version version string (semver)
 * @param skillId skill identifier
 * @param createdAt creation timestamp
 * @param createdBy user who created this version
 * @param changeLog description of changes
 * @param checksum content checksum
 * @param isCurrent whether this is the current version
 * @param parentVersion parent version (for rollback chain)
 */
public record SkillVersion(
    String version,
    String skillId,
    Instant createdAt,
    String createdBy,
    String changeLog,
    String checksum,
    boolean isCurrent,
    String parentVersion
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private String skillId;
        private Instant createdAt = Instant.now();
        private String createdBy = "system";
        private String changeLog = "";
        private String checksum;
        private boolean isCurrent = true;
        private String parentVersion;

        public Builder version(String version) { this.version = version; return this; }
        public Builder skillId(String skillId) { this.skillId = skillId; return this; }
        public Builder createdAt(Instant instant) { this.createdAt = instant; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder changeLog(String changeLog) { this.changeLog = changeLog; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }
        public Builder isCurrent(boolean isCurrent) { this.isCurrent = isCurrent; return this; }
        public Builder parentVersion(String parentVersion) { this.parentVersion = parentVersion; return this; }

        public SkillVersion build() {
            return new SkillVersion(
                version, skillId, createdAt, createdBy, changeLog,
                checksum, isCurrent, parentVersion
            );
        }
    }
}
