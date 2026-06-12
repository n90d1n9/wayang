package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of one deployment event.
 */
public record SkillManagementAdminDeploymentHistoryEntry(
        String occurredAt,
        String operationId,
        String parentOperationId,
        boolean success,
        boolean dryRun,
        boolean changed,
        boolean consistent,
        int definitionChanges,
        int artifactChanges,
        int artifactConflicts,
        int lifecycleCreated,
        int lifecycleRemoved,
        boolean eventPruneEnabled,
        boolean eventPruneSkipped,
        boolean eventPruneChanged,
        int eventPruned,
        boolean preflightAvailable,
        boolean preflightReady,
        boolean preflightDeployable,
        int preflightErrors,
        int preflightConfigurationErrors,
        int preflightTargetStoreErrors,
        int preflightSourceStoreErrors,
        int preflightCapabilityErrors,
        String preflightMessage,
        String errorType,
        String error,
        List<SkillManagementAdminMaintenanceStepReport> steps) {

    public static SkillManagementAdminDeploymentHistoryEntry from(SkillManagementEvent event) {
        return SkillManagementAdminDeploymentHistoryEntryAttributes.from(event);
    }

    public SkillManagementAdminDeploymentHistoryEntry(
            String occurredAt,
            String operationId,
            String parentOperationId,
            boolean success,
            boolean dryRun,
            boolean changed,
            boolean consistent,
            int definitionChanges,
            int artifactChanges,
            int artifactConflicts,
            int lifecycleCreated,
            int lifecycleRemoved,
            boolean eventPruneEnabled,
            boolean eventPruneSkipped,
            boolean eventPruneChanged,
            int eventPruned,
            boolean preflightAvailable,
            boolean preflightReady,
            boolean preflightDeployable,
            int preflightErrors,
            int preflightConfigurationErrors,
            int preflightTargetStoreErrors,
            int preflightSourceStoreErrors,
            int preflightCapabilityErrors,
            String preflightMessage,
            String errorType,
            String error) {
        this(
                occurredAt,
                operationId,
                parentOperationId,
                success,
                dryRun,
                changed,
                consistent,
                definitionChanges,
                artifactChanges,
                artifactConflicts,
                lifecycleCreated,
                lifecycleRemoved,
                eventPruneEnabled,
                eventPruneSkipped,
                eventPruneChanged,
                eventPruned,
                preflightAvailable,
                preflightReady,
                preflightDeployable,
                preflightErrors,
                preflightConfigurationErrors,
                preflightTargetStoreErrors,
                preflightSourceStoreErrors,
                preflightCapabilityErrors,
                preflightMessage,
                errorType,
                error,
                List.of());
    }

    public SkillManagementAdminDeploymentHistoryEntry {
        occurredAt = SkillManagementAdminValueSupport.text(occurredAt);
        operationId = SkillManagementAdminValueSupport.identifier(operationId);
        parentOperationId = SkillManagementAdminValueSupport.identifier(parentOperationId);
        definitionChanges = SkillManagementAdminValueSupport.nonNegative(definitionChanges);
        artifactChanges = SkillManagementAdminValueSupport.nonNegative(artifactChanges);
        artifactConflicts = SkillManagementAdminValueSupport.nonNegative(artifactConflicts);
        lifecycleCreated = SkillManagementAdminValueSupport.nonNegative(lifecycleCreated);
        lifecycleRemoved = SkillManagementAdminValueSupport.nonNegative(lifecycleRemoved);
        eventPruned = SkillManagementAdminValueSupport.nonNegative(eventPruned);
        preflightErrors = SkillManagementAdminValueSupport.nonNegative(preflightErrors);
        preflightConfigurationErrors = SkillManagementAdminValueSupport.nonNegative(preflightConfigurationErrors);
        preflightTargetStoreErrors = SkillManagementAdminValueSupport.nonNegative(preflightTargetStoreErrors);
        preflightSourceStoreErrors = SkillManagementAdminValueSupport.nonNegative(preflightSourceStoreErrors);
        preflightCapabilityErrors = SkillManagementAdminValueSupport.nonNegative(preflightCapabilityErrors);
        preflightMessage = SkillManagementAdminValueSupport.text(preflightMessage);
        errorType = SkillManagementAdminValueSupport.text(errorType);
        error = SkillManagementAdminValueSupport.text(error);
        steps = SkillManagementAdminValueSupport.nonNullList(steps);
    }
}
