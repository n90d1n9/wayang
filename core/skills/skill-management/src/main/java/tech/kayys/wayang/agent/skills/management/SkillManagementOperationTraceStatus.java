package tech.kayys.wayang.agent.skills.management;

import java.util.Locale;
import java.util.Optional;

/**
 * Coarse status for an operation trace assembled from correlated admin events.
 */
public enum SkillManagementOperationTraceStatus {
    MISSING_OPERATION_ID,
    ROOT_MISSING,
    FAILED,
    HEALTHY;

    public static SkillManagementOperationTraceStatus from(
            String operationId,
            boolean rootEventAvailable,
            boolean failed) {
        if (SkillManagementValueSupport.identifier(operationId).isBlank()) {
            return MISSING_OPERATION_ID;
        }
        if (!rootEventAvailable) {
            return ROOT_MISSING;
        }
        return failed ? FAILED : HEALTHY;
    }

    public static Optional<SkillManagementOperationTraceStatus> parse(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        for (SkillManagementOperationTraceStatus status : values()) {
            if (status.name().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }

    public static SkillManagementOperationTraceStatus require(String value) {
        return parse(value).orElseThrow(
                () -> new IllegalArgumentException("Unknown operation trace status: " + value));
    }

    private static String normalize(String value) {
        return SkillManagementValueSupport.identifier(value)
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
