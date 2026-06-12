package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Admin deployment-history entry fields decoded from raw event attributes.
 */
record SkillManagementAdminDeploymentHistoryEntryAttributes(
        String operationId,
        String parentOperationId,
        SkillManagementAdminDeploymentHistoryChangeAttributes changes,
        SkillManagementAdminDeploymentHistoryEventPruneAttributes eventPrune,
        SkillManagementAdminDeploymentHistoryPreflightAttributes preflight,
        String errorType,
        String error,
        List<SkillManagementAdminMaintenanceStepReport> steps) {

    SkillManagementAdminDeploymentHistoryEntryAttributes {
        operationId = SkillManagementAdminValueSupport.identifier(operationId);
        parentOperationId = SkillManagementAdminValueSupport.identifier(parentOperationId);
        changes = changes == null ? SkillManagementAdminDeploymentHistoryChangeAttributes.empty() : changes;
        eventPrune = eventPrune == null ? SkillManagementAdminDeploymentHistoryEventPruneAttributes.empty() : eventPrune;
        preflight = preflight == null ? SkillManagementAdminDeploymentHistoryPreflightAttributes.empty() : preflight;
        errorType = SkillManagementAdminValueSupport.text(errorType);
        error = SkillManagementAdminValueSupport.text(error);
        steps = SkillManagementAdminValueSupport.nonNullList(steps);
    }

    static SkillManagementAdminDeploymentHistoryEntry from(SkillManagementEvent event) {
        Objects.requireNonNull(event, "event");
        SkillManagementAdminDeploymentHistoryEntryAttributes attributes =
                from(SkillManagementEventAttributeReader.from(event));
        return attributes.entry(event.occurredAt().toString(), event.success());
    }

    static SkillManagementAdminDeploymentHistoryEntryAttributes from(
            SkillManagementEventAttributeReader attributes) {
        SkillManagementEventAttributeReader reader = SkillManagementEventAttributeReader.orEmpty(attributes);
        return new SkillManagementAdminDeploymentHistoryEntryAttributes(
                reader.operationId(),
                reader.parentOperationId(),
                SkillManagementAdminDeploymentHistoryChangeAttributes.from(reader),
                SkillManagementAdminDeploymentHistoryEventPruneAttributes.from(reader),
                SkillManagementAdminDeploymentHistoryPreflightAttributes.from(reader),
                reader.text(ERROR_TYPE),
                reader.text(ERROR),
                SkillManagementMaintenanceStepEventAttributes.adminReports(reader));
    }

    SkillManagementAdminDeploymentHistoryEntry entry(String occurredAt, boolean success) {
        return new SkillManagementAdminDeploymentHistoryEntry(
                occurredAt,
                operationId,
                parentOperationId,
                success,
                changes.dryRun(),
                changes.changed(),
                changes.consistent(),
                changes.definitionChanges(),
                changes.artifactChanges(),
                changes.artifactConflicts(),
                changes.lifecycleCreated(),
                changes.lifecycleRemoved(),
                eventPrune.enabled(),
                eventPrune.skipped(),
                eventPrune.changed(),
                eventPrune.pruned(),
                preflight.available(),
                preflight.ready(),
                preflight.deployable(),
                preflight.errors(),
                preflight.configurationErrors(),
                preflight.targetStoreErrors(),
                preflight.sourceStoreErrors(),
                preflight.capabilityErrors(),
                preflight.message(),
                errorType,
                error,
                steps);
    }
}
