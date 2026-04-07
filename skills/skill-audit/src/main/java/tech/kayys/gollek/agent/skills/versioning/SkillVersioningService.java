package tech.kayys.gollek.agent.skills.versioning;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.agent.skills.repo.spi.SkillContent;
import tech.kayys.gollek.agent.skills.repo.spi.SkillMetadata;
import tech.kayys.gollek.agent.skills.repo.spi.SkillRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skill versioning service with automatic versioning and rollback capabilities.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic version management (semver)</li>
 *   <li>Version history tracking</li>
 *   <li>Rollback to any previous version</li>
 *   <li>Version comparison</li>
 *   <li>Change log management</li>
 * </ul>
 */
@ApplicationScoped
public class SkillVersioningService {

    private static final Logger log = LoggerFactory.getLogger(SkillVersioningService.class);

    private final Map<String, List<SkillVersion>> versionHistory;
    private final Map<String, SkillContent> versionContents;

    public SkillVersioningService() {
        this.versionHistory = new ConcurrentHashMap<>();
        this.versionContents = new ConcurrentHashMap<>();
    }

    /**
     * Create a new version when skill is updated.
     */
    public SkillVersion createVersion(SkillContent content, String userId, String changeLog) {
        String skillId = content.metadata().id();
        String currentVersion = content.metadata().version();

        // Parse and increment version
        String newVersion = incrementVersion(currentVersion);

        SkillVersion version = SkillVersion.builder()
                .version(newVersion)
                .skillId(skillId)
                .createdBy(userId)
                .changeLog(changeLog)
                .checksum(content.metadata().checksum())
                .isCurrent(true)
                .parentVersion(currentVersion)
                .build();

        // Store version
        versionHistory.computeIfAbsent(skillId, k -> new ArrayList<>()).add(version);
        versionContents.put(skillId + "@" + newVersion, content);

        // Mark previous versions as not current
        List<SkillVersion> versions = versionHistory.get(skillId);
        for (SkillVersion v : versions) {
            if (!v.version().equals(newVersion)) {
                // Create updated version with isCurrent=false
                int idx = versions.indexOf(v);
                versions.set(idx, new SkillVersion(
                    v.version(), v.skillId(), v.createdAt(), v.createdBy(),
                    v.changeLog(), v.checksum(), false, v.parentVersion()
                ));
            }
        }

        log.info("Created version {} for skill {}", newVersion, skillId);
        return version;
    }

    /**
     * Get all versions of a skill.
     */
    public List<SkillVersion> getVersions(String skillId) {
        return versionHistory.getOrDefault(skillId, List.of());
    }

    /**
     * Get specific version of a skill.
     */
    public Optional<SkillContent> getVersion(String skillId, String version) {
        String key = skillId + "@" + version;
        return Optional.ofNullable(versionContents.get(key));
    }

    /**
     * Get current version of a skill.
     */
    public Optional<SkillVersion> getCurrentVersion(String skillId) {
        List<SkillVersion> versions = versionHistory.get(skillId);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }

        return versions.stream()
                .filter(SkillVersion::isCurrent)
                .findFirst();
    }

    /**
     * Rollback to a previous version.
     */
    public SkillContent rollback(String skillId, String targetVersion, 
                                 SkillRepository repository, String userId) {
        // Get target version content
        Optional<SkillContent> targetContent = getVersion(skillId, targetVersion);
        if (targetContent.isEmpty()) {
            throw new VersionNotFoundException("Version " + targetVersion + " not found for skill " + skillId);
        }

        // Create rollback version
        SkillContent currentContent = repository.get(skillId)
                .await().indefinitely()
                .orElseThrow(() -> new VersionNotFoundException("Current skill not found"));

        String changeLog = "Rollback to version " + targetVersion;
        SkillVersion rollbackVersion = createVersion(currentContent, userId, changeLog);

        // Save target version as current
        SkillContent versionedContent = new SkillContent.Builder()
                .from(targetContent.get())
                .metadata(new SkillMetadata.Builder()
                        .from(targetContent.get().metadata())
                        .version(rollbackVersion.version())
                        .updatedAt(Instant.now())
                        .build())
                .build();

        repository.save(versionedContent).await().indefinitely();

        log.info("Rolled back skill {} to version {}", skillId, targetVersion);
        return versionedContent;
    }

    /**
     * Compare two versions.
     */
    public int compareVersions(String version1, String version2) {
        return compareSemver(version1, version2);
    }

    /**
     * Get version history size.
     */
    public int getVersionCount(String skillId) {
        return versionHistory.getOrDefault(skillId, List.of()).size();
    }

    /**
     * Clear version history for a skill.
     */
    public void clearHistory(String skillId) {
        versionHistory.remove(skillId);
        // Clear version contents
        versionContents.keySet().removeIf(key -> key.startsWith(skillId + "@"));
        log.info("Cleared version history for skill: {}", skillId);
    }

    // ==================== Private Helpers ====================

    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "1.0.0";
        }

        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 3) {
                int patch = Integer.parseInt(parts[2].split("-")[0]);
                parts[2] = (patch + 1) + (parts[2].contains("-") ? "-" + parts[2].split("-")[1] : "");
                return String.join(".", parts);
            }
        } catch (Exception e) {
            log.warn("Failed to parse version: {}, defaulting to 1.0.0", currentVersion);
        }

        return "1.0.0";
    }

    private int compareSemver(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.|-");
            String[] parts2 = v2.split("\\.|-");

            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int num1 = Integer.parseInt(parts1[i].replaceAll("[^0-9]", ""));
                int num2 = Integer.parseInt(parts2[i].replaceAll("[^0-9]", ""));

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }

            return Integer.compare(parts1.length, parts2.length);
        } catch (Exception e) {
            return v1.compareTo(v2);
        }
    }

    /**
     * Version not found exception.
     */
    public static class VersionNotFoundException extends RuntimeException {
        public VersionNotFoundException(String message) {
            super(message);
        }
    }
}
