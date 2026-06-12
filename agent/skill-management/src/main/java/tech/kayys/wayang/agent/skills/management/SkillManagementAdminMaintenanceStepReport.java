package tech.kayys.wayang.agent.skills.management;

/**
 * Stable admin-facing projection of one maintenance step diagnostic.
 */
public record SkillManagementAdminMaintenanceStepReport(
        String step,
        String status,
        boolean dryRun,
        boolean skipped,
        boolean changed,
        boolean consistent,
        long changes,
        long conflicts,
        String failure) {

    public SkillManagementAdminMaintenanceStepReport {
        step = SkillManagementAdminValueSupport.identifier(step);
        status = SkillManagementAdminValueSupport.unknownIfBlank(status);
        changes = SkillManagementAdminValueSupport.nonNegative(changes);
        conflicts = SkillManagementAdminValueSupport.nonNegative(conflicts);
        failure = SkillManagementAdminValueSupport.blankToEmpty(failure);
    }
}
