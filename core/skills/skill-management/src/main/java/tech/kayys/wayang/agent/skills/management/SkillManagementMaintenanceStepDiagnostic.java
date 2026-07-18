package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Stable per-step diagnostic for maintenance reporting and harness checks.
 */
public record SkillManagementMaintenanceStepDiagnostic(
        SkillManagementMaintenanceStep step,
        SkillManagementMaintenanceStepStatus status,
        boolean dryRun,
        boolean skipped,
        boolean changed,
        boolean consistent,
        long changes,
        long conflicts,
        String failure) {

    public SkillManagementMaintenanceStepDiagnostic {
        step = Objects.requireNonNull(step, "step");
        status = Objects.requireNonNull(status, "status");
        changes = SkillManagementValueSupport.nonNegative(changes);
        conflicts = SkillManagementValueSupport.nonNegative(conflicts);
        failure = SkillManagementValueSupport.blankToEmpty(failure);
    }

    public boolean successful() {
        return status != SkillManagementMaintenanceStepStatus.FAILED;
    }
}
