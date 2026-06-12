package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Shared filename normalization for skill-addressable filesystem stores.
 */
final class SkillManagementFileNames {

    private SkillManagementFileNames() {
    }

    static Path skillFile(Path directory, String skillId, String extension, String context) {
        return Objects.requireNonNull(directory, "directory")
                .resolve(normalizeSkillId(skillId, context)
                        + Objects.requireNonNull(extension, "extension"));
    }

    static String normalizeSkillId(String skillId, String context) {
        return SkillManagementSkillIds.normalizeForStorage(skillId, context);
    }
}
