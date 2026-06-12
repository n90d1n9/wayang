package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Compact aggregate view of a maintenance run.
 */
public record SkillManagementMaintenanceSummary(
        boolean dryRun,
        boolean changed,
        boolean consistent,
        boolean definitionChanged,
        long definitionChanges,
        long definitionConflicts,
        boolean artifactChanged,
        long artifactChanges,
        long artifactConflicts,
        boolean lifecycleChanged,
        int lifecycleCreated,
        int lifecycleRemoved,
        boolean lifecycleConsistent,
        boolean eventPruneSkipped,
        boolean eventPruneChanged,
        int eventPruned,
        boolean eventPruneSuccess) {

    public SkillManagementMaintenanceSummary {
        definitionChanges = SkillManagementValueSupport.nonNegative(definitionChanges);
        definitionConflicts = SkillManagementValueSupport.nonNegative(definitionConflicts);
        artifactChanges = SkillManagementValueSupport.nonNegative(artifactChanges);
        artifactConflicts = SkillManagementValueSupport.nonNegative(artifactConflicts);
        lifecycleCreated = SkillManagementValueSupport.nonNegative(lifecycleCreated);
        lifecycleRemoved = SkillManagementValueSupport.nonNegative(lifecycleRemoved);
        eventPruned = SkillManagementValueSupport.nonNegative(eventPruned);
    }

    static SkillManagementMaintenanceSummary from(SkillManagementMaintenanceResult result) {
        Objects.requireNonNull(result, "result");
        SkillDefinitionStoreSyncResult definitionSync = result.definitionSyncResult();
        SkillArtifactStoreSyncResult artifactSync = result.artifactSyncResult();
        SkillLifecycleStateReconcileResult lifecycle = result.lifecycleStateReconcileResult();
        SkillManagementEventPruneResult eventPrune = result.eventPruneResult();
        long definitionChanges = definitionSync.changed();
        long artifactChanges = artifactSync.changed();
        int lifecycleCreated = lifecycle.createdStateSkillIds().size();
        int lifecycleRemoved = lifecycle.removedStateSkillIds().size();
        boolean lifecycleChanged = lifecycleCreated > 0 || lifecycleRemoved > 0;
        boolean eventPruneSuccess = eventPrune.success();
        boolean dryRun = definitionSync.dryRun()
                && artifactSync.dryRun()
                && !lifecycleChanged
                && (eventPrune.skipped() || eventPrune.dryRun());
        boolean changed = definitionChanges > 0
                || artifactChanges > 0
                || lifecycleChanged
                || eventPrune.changed();
        boolean consistent = definitionSync.conflicts() == 0
                && artifactSync.conflicts() == 0
                && lifecycle.consistent()
                && eventPruneSuccess;
        return new SkillManagementMaintenanceSummary(
                dryRun,
                changed,
                consistent,
                definitionChanges > 0,
                definitionChanges,
                definitionSync.conflicts(),
                artifactChanges > 0,
                artifactChanges,
                artifactSync.conflicts(),
                lifecycleChanged,
                lifecycleCreated,
                lifecycleRemoved,
                lifecycle.consistent(),
                eventPrune.skipped(),
                eventPrune.changed(),
                eventPrune.prunedEvents(),
                eventPruneSuccess);
    }
}
