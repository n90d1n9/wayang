package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Result of a combined skill persistence and event-history maintenance run.
 */
public record SkillManagementMaintenanceResult(
        SkillDefinitionStoreSyncResult definitionSyncResult,
        SkillArtifactStoreSyncResult artifactSyncResult,
        SkillLifecycleStateReconcileResult lifecycleStateReconcileResult,
        SkillManagementEventPruneResult eventPruneResult) {

    public SkillManagementMaintenanceResult(
            SkillDefinitionStoreSyncResult definitionSyncResult,
            SkillLifecycleStateReconcileResult lifecycleStateReconcileResult) {
        this(definitionSyncResult, emptyArtifactSyncResult(), lifecycleStateReconcileResult,
                SkillManagementEventPruneResult.skippedResult());
    }

    public SkillManagementMaintenanceResult(
            SkillDefinitionStoreSyncResult definitionSyncResult,
            SkillLifecycleStateReconcileResult lifecycleStateReconcileResult,
            SkillManagementEventPruneResult eventPruneResult) {
        this(definitionSyncResult, emptyArtifactSyncResult(), lifecycleStateReconcileResult, eventPruneResult);
    }

    public SkillManagementMaintenanceResult {
        definitionSyncResult = Objects.requireNonNull(definitionSyncResult, "definitionSyncResult");
        artifactSyncResult = Objects.requireNonNull(artifactSyncResult, "artifactSyncResult");
        lifecycleStateReconcileResult =
                Objects.requireNonNull(lifecycleStateReconcileResult, "lifecycleStateReconcileResult");
        eventPruneResult = Objects.requireNonNull(eventPruneResult, "eventPruneResult");
    }

    public SkillManagementMaintenanceSummary summary() {
        return SkillManagementMaintenanceSummary.from(this);
    }

    public List<SkillManagementMaintenanceStepDiagnostic> stepDiagnostics() {
        return SkillManagementMaintenanceStepDiagnostics.from(this);
    }

    public boolean dryRun() {
        return summary().dryRun();
    }

    public boolean changed() {
        return summary().changed();
    }

    public boolean consistent() {
        return summary().consistent();
    }

    private static SkillArtifactStoreSyncResult emptyArtifactSyncResult() {
        return new SkillArtifactStoreSyncResult(true, List.of());
    }
}
