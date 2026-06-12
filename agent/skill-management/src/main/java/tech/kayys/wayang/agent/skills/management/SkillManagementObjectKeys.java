package tech.kayys.wayang.agent.skills.management;

/**
 * Shared key normalization for object-store-backed skill-management stores.
 */
final class SkillManagementObjectKeys {

    private SkillManagementObjectKeys() {
    }

    static String normalizePrefix(String prefix, String defaultPrefix) {
        String normalized = prefix == null || prefix.isBlank() ? defaultPrefix : prefix.trim();
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    static String skillKey(String prefix, String skillId, String extension, String context) {
        return prefix + normalizeSkillId(skillId, context) + extension;
    }

    static String normalizeSkillId(String skillId, String context) {
        return SkillManagementSkillIds.normalizeForStorage(skillId, "object storage " + context);
    }
}
