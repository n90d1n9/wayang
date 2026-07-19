package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Stable admin-facing projection of a deployment result.
 */
public record SkillManagementAdminDeploymentReport(
        boolean dryRun,
        boolean changed,
        boolean consistent,
        SkillManagementAdminMaintenanceReport maintenance) {

    public SkillManagementAdminDeploymentReport(SkillManagementAdminMaintenanceReport maintenance) {
        this(false, false, false, maintenance);
    }

    public SkillManagementAdminDeploymentReport {
        maintenance = Objects.requireNonNull(maintenance, "maintenance");
        dryRun = maintenance.dryRun();
        changed = maintenance.changed();
        consistent = maintenance.consistent();
    }
}
