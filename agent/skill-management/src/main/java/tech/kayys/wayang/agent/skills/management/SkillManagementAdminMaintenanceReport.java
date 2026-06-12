package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Stable admin-facing projection of a maintenance workflow result.
 */
public record SkillManagementAdminMaintenanceReport(
        boolean dryRun,
        boolean changed,
        boolean consistent,
        SkillManagementAdminDefinitionSyncStatus definitionSync,
        SkillManagementAdminArtifactSyncStatus artifactSync,
        SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
        SkillManagementAdminEventPruneReport eventPrune,
        List<SkillManagementAdminMaintenanceStepReport> steps) {

    public SkillManagementAdminMaintenanceReport(
            boolean dryRun,
            boolean changed,
            boolean consistent,
            SkillManagementAdminDefinitionSyncStatus definitionSync,
            SkillManagementAdminArtifactSyncStatus artifactSync,
            SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
            SkillManagementAdminEventPruneReport eventPrune) {
        this(
                dryRun,
                changed,
                consistent,
                definitionSync,
                artifactSync,
                lifecycleStateReconciliation,
                eventPrune,
                List.of());
    }

    public SkillManagementAdminMaintenanceReport(
            SkillManagementAdminDefinitionSyncStatus definitionSync,
            SkillManagementAdminArtifactSyncStatus artifactSync,
            SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
            SkillManagementAdminEventPruneReport eventPrune) {
        this(
                definitionSync,
                artifactSync,
                lifecycleStateReconciliation,
                eventPrune,
                List.of());
    }

    public SkillManagementAdminMaintenanceReport(
            SkillManagementAdminDefinitionSyncStatus definitionSync,
            SkillManagementAdminArtifactSyncStatus artifactSync,
            SkillManagementAdminReconcileStatus lifecycleStateReconciliation,
            SkillManagementAdminEventPruneReport eventPrune,
            List<SkillManagementAdminMaintenanceStepReport> steps) {
        this(
                false,
                false,
                false,
                definitionSync,
                artifactSync,
                lifecycleStateReconciliation,
                eventPrune,
                steps);
    }

    public SkillManagementAdminMaintenanceReport {
        definitionSync = Objects.requireNonNull(definitionSync, "definitionSync");
        artifactSync = Objects.requireNonNull(artifactSync, "artifactSync");
        lifecycleStateReconciliation =
                Objects.requireNonNull(lifecycleStateReconciliation, "lifecycleStateReconciliation");
        eventPrune = Objects.requireNonNull(eventPrune, "eventPrune");
        steps = SkillManagementAdminValueSupport.nonNullList(steps);
        dryRun = definitionSync.dryRun()
                && artifactSync.dryRun()
                && !lifecycleStateReconciliation.changed()
                && (eventPrune.skipped() || eventPrune.dryRun());
        changed = definitionSync.changed() > 0
                || artifactSync.changed() > 0
                || lifecycleStateReconciliation.changed()
                || eventPrune.changed();
        consistent = definitionSync.conflicts() == 0
                && artifactSync.conflicts() == 0
                && lifecycleStateReconciliation.consistent()
                && eventPrune.success();
    }
}
