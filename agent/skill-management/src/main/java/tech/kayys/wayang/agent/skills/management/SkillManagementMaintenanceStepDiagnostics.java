package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Builds ordered diagnostics for the maintenance result steps.
 */
final class SkillManagementMaintenanceStepDiagnostics {

    private SkillManagementMaintenanceStepDiagnostics() {
    }

    static List<SkillManagementMaintenanceStepDiagnostic> from(SkillManagementMaintenanceResult result) {
        Objects.requireNonNull(result, "result");
        return List.of(
                definitionSync(result.definitionSyncResult()),
                artifactSync(result.artifactSyncResult()),
                lifecycleReconcile(result.lifecycleStateReconcileResult()),
                eventPrune(result.eventPruneResult()));
    }

    private static SkillManagementMaintenanceStepDiagnostic definitionSync(
            SkillDefinitionStoreSyncResult result) {
        long changes = result.changed();
        long conflicts = result.conflicts();
        return new SkillManagementMaintenanceStepDiagnostic(
                SkillManagementMaintenanceStep.DEFINITION_SYNC,
                syncStatus(result.dryRun(), changes, conflicts),
                result.dryRun(),
                false,
                changes > 0,
                conflicts == 0,
                changes,
                conflicts,
                "");
    }

    private static SkillManagementMaintenanceStepDiagnostic artifactSync(
            SkillArtifactStoreSyncResult result) {
        long changes = result.changed();
        long conflicts = result.conflicts();
        return new SkillManagementMaintenanceStepDiagnostic(
                SkillManagementMaintenanceStep.ARTIFACT_SYNC,
                syncStatus(result.dryRun(), changes, conflicts),
                result.dryRun(),
                false,
                changes > 0,
                conflicts == 0,
                changes,
                conflicts,
                "");
    }

    private static SkillManagementMaintenanceStepStatus syncStatus(
            boolean dryRun,
            long changes,
            long conflicts) {
        if (conflicts > 0) {
            return SkillManagementMaintenanceStepStatus.CONFLICT;
        }
        if (dryRun) {
            return SkillManagementMaintenanceStepStatus.DRY_RUN;
        }
        if (changes > 0) {
            return SkillManagementMaintenanceStepStatus.CHANGED;
        }
        return SkillManagementMaintenanceStepStatus.UNCHANGED;
    }

    private static SkillManagementMaintenanceStepDiagnostic lifecycleReconcile(
            SkillLifecycleStateReconcileResult result) {
        int created = result.createdStateSkillIds().size();
        int removed = result.removedStateSkillIds().size();
        long changes = created + removed;
        long unresolved = unresolved(result.missingStateSkillIds(), result.createdStateSkillIds())
                + unresolved(result.orphanedStateSkillIds(), result.removedStateSkillIds());
        return new SkillManagementMaintenanceStepDiagnostic(
                SkillManagementMaintenanceStep.LIFECYCLE_RECONCILE,
                lifecycleStatus(changes, unresolved),
                false,
                false,
                changes > 0,
                unresolved == 0,
                changes,
                unresolved,
                "");
    }

    private static SkillManagementMaintenanceStepStatus lifecycleStatus(
            long changes,
            long unresolved) {
        if (unresolved > 0) {
            return SkillManagementMaintenanceStepStatus.INCONSISTENT;
        }
        if (changes > 0) {
            return SkillManagementMaintenanceStepStatus.CHANGED;
        }
        return SkillManagementMaintenanceStepStatus.UNCHANGED;
    }

    private static long unresolved(List<String> expected, List<String> resolved) {
        return expected.stream()
                .filter(skillId -> !resolved.contains(skillId))
                .count();
    }

    private static SkillManagementMaintenanceStepDiagnostic eventPrune(
            SkillManagementEventPruneResult result) {
        boolean success = result.success();
        return new SkillManagementMaintenanceStepDiagnostic(
                SkillManagementMaintenanceStep.EVENT_PRUNE,
                eventPruneStatus(result, success),
                result.dryRun(),
                result.skipped(),
                result.changed(),
                success,
                result.prunedEvents(),
                success ? 0 : 1,
                result.failure());
    }

    private static SkillManagementMaintenanceStepStatus eventPruneStatus(
            SkillManagementEventPruneResult result,
            boolean success) {
        if (!success) {
            return SkillManagementMaintenanceStepStatus.FAILED;
        }
        if (result.skipped()) {
            return SkillManagementMaintenanceStepStatus.SKIPPED;
        }
        if (result.dryRun()) {
            return SkillManagementMaintenanceStepStatus.DRY_RUN;
        }
        if (result.changed()) {
            return SkillManagementMaintenanceStepStatus.CHANGED;
        }
        return SkillManagementMaintenanceStepStatus.UNCHANGED;
    }
}
