package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of lifecycle state reconciliation.
 */
public record SkillManagementAdminReconcileStatus(
        boolean consistent,
        List<String> definitionSkillIds,
        List<String> persistedStateSkillIds,
        List<String> missingStateSkillIds,
        List<String> orphanedStateSkillIds,
        List<String> createdStateSkillIds,
        List<String> removedStateSkillIds,
        String failure) {

    public SkillManagementAdminReconcileStatus(
            List<String> definitionSkillIds,
            List<String> persistedStateSkillIds,
            List<String> missingStateSkillIds,
            List<String> orphanedStateSkillIds,
            List<String> createdStateSkillIds,
            List<String> removedStateSkillIds,
            String failure) {
        this(
                false,
                definitionSkillIds,
                persistedStateSkillIds,
                missingStateSkillIds,
                orphanedStateSkillIds,
                createdStateSkillIds,
                removedStateSkillIds,
                failure);
    }

    public SkillManagementAdminReconcileStatus {
        definitionSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(definitionSkillIds);
        persistedStateSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(persistedStateSkillIds);
        missingStateSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(missingStateSkillIds);
        orphanedStateSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(orphanedStateSkillIds);
        createdStateSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(createdStateSkillIds);
        removedStateSkillIds = SkillManagementAdminValueSupport.sortedDistinctStrings(removedStateSkillIds);
        failure = SkillManagementAdminValueSupport.blankToEmpty(failure);
        consistent = failure.isBlank()
                && createdStateSkillIds.containsAll(missingStateSkillIds)
                && removedStateSkillIds.containsAll(orphanedStateSkillIds);
    }

    public boolean changed() {
        return !createdStateSkillIds.isEmpty() || !removedStateSkillIds.isEmpty();
    }
}
