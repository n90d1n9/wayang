package tech.kayys.wayang.agent.skills.management;

/**
 * Shared skill-id normalization rules for persistence backends.
 */
final class SkillManagementSkillIds {

    private SkillManagementSkillIds() {
    }

    static boolean isBlank(String skillId) {
        return skillId == null || skillId.isBlank();
    }

    static String normalizeForStorage(String skillId, String context) {
        String normalized = skillId == null ? "" : skillId.trim();
        if (isBlank(normalized)
                || normalized.contains("/")
                || normalized.contains("\\")
                || normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid skill id for " + context + ": " + skillId);
        }
        return normalized;
    }
}
