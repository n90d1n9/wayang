package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.function.Predicate;

/**
 * Aggregate metrics for one grouped maintenance step in deployment history.
 */
record SkillManagementAdminMaintenanceStepHistoryStats(
        int deployments,
        int dryRunDeployments,
        int skippedDeployments,
        int changedDeployments,
        int consistentDeployments,
        int failedDeployments,
        long changes,
        long conflicts) {

    SkillManagementAdminMaintenanceStepHistoryStats {
        deployments = SkillManagementAdminValueSupport.nonNegative(deployments);
        dryRunDeployments = SkillManagementAdminValueSupport.nonNegative(dryRunDeployments);
        skippedDeployments = SkillManagementAdminValueSupport.nonNegative(skippedDeployments);
        changedDeployments = SkillManagementAdminValueSupport.nonNegative(changedDeployments);
        consistentDeployments = SkillManagementAdminValueSupport.nonNegative(consistentDeployments);
        failedDeployments = SkillManagementAdminValueSupport.nonNegative(failedDeployments);
        changes = SkillManagementAdminValueSupport.nonNegative(changes);
        conflicts = SkillManagementAdminValueSupport.nonNegative(conflicts);
    }

    static SkillManagementAdminMaintenanceStepHistoryStats from(
            List<SkillManagementAdminMaintenanceStepReport> reports) {
        List<SkillManagementAdminMaintenanceStepReport> entries =
                SkillManagementAdminValueSupport.nonNullList(reports);
        return new SkillManagementAdminMaintenanceStepHistoryStats(
                entries.size(),
                count(entries, SkillManagementAdminMaintenanceStepReport::dryRun),
                count(entries, SkillManagementAdminMaintenanceStepReport::skipped),
                count(entries, SkillManagementAdminMaintenanceStepReport::changed),
                count(entries, SkillManagementAdminMaintenanceStepReport::consistent),
                count(entries, SkillManagementAdminMaintenanceStepReportStatuses::failed),
                entries.stream().mapToLong(SkillManagementAdminMaintenanceStepReport::changes).sum(),
                entries.stream().mapToLong(SkillManagementAdminMaintenanceStepReport::conflicts).sum());
    }

    SkillManagementAdminMaintenanceStepHistorySummary summary(String step) {
        return new SkillManagementAdminMaintenanceStepHistorySummary(
                step,
                deployments,
                dryRunDeployments,
                skippedDeployments,
                changedDeployments,
                consistentDeployments,
                failedDeployments,
                changes,
                conflicts);
    }

    private static int count(
            List<SkillManagementAdminMaintenanceStepReport> reports,
            Predicate<SkillManagementAdminMaintenanceStepReport> predicate) {
        return SkillManagementAdminValueSupport.countMatching(reports, predicate);
    }
}
