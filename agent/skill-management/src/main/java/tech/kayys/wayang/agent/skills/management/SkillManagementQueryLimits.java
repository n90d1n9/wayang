package tech.kayys.wayang.agent.skills.management;

/**
 * Shared limit normalization for bounded skill-management query windows.
 */
final class SkillManagementQueryLimits {

    static final int DEFAULT_LIMIT = 100;
    static final int MAX_LIMIT = 1_000;

    private SkillManagementQueryLimits() {
    }

    static int normalize(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }
}
