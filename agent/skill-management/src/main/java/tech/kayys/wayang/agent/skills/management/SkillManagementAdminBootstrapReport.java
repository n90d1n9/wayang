package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Stable admin-facing projection of skill-management bootstrap output.
 */
public record SkillManagementAdminBootstrapReport(
        boolean ready,
        boolean lifecycleStateConsistent,
        boolean lifecycleStateChanged,
        SkillManagementAdminInspection initialInspection,
        SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
        SkillManagementAdminInspection finalInspection) {

    public SkillManagementAdminBootstrapReport(
            SkillManagementAdminInspection initialInspection,
            SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
            SkillManagementAdminInspection finalInspection) {
        this(false, false, false, initialInspection, lifecycleStateReconciliation, finalInspection);
    }

    public SkillManagementAdminBootstrapReport {
        initialInspection = Objects.requireNonNull(initialInspection, "initialInspection");
        lifecycleStateReconciliation =
                Objects.requireNonNull(lifecycleStateReconciliation, "lifecycleStateReconciliation");
        finalInspection = Objects.requireNonNull(finalInspection, "finalInspection");
        ready = finalInspection.ready();
        lifecycleStateConsistent = finalInspection.lifecycleStateConsistent();
        lifecycleStateChanged = lifecycleStateReconciliation.changed();
    }
}
