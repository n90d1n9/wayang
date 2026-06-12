package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Summary of a lifecycle state reconciliation run.
 */
public record SkillLifecycleStateReconcileResult(
        List<String> definitionSkillIds,
        List<String> persistedStateSkillIds,
        List<String> missingStateSkillIds,
        List<String> orphanedStateSkillIds,
        List<String> createdStateSkillIds,
        List<String> removedStateSkillIds) {

    public SkillLifecycleStateReconcileResult {
        definitionSkillIds = copy(definitionSkillIds);
        persistedStateSkillIds = copy(persistedStateSkillIds);
        missingStateSkillIds = copy(missingStateSkillIds);
        orphanedStateSkillIds = copy(orphanedStateSkillIds);
        createdStateSkillIds = copy(createdStateSkillIds);
        removedStateSkillIds = copy(removedStateSkillIds);
    }

    public boolean consistent() {
        return createdStateSkillIds.containsAll(missingStateSkillIds)
                && removedStateSkillIds.containsAll(orphanedStateSkillIds);
    }

    private static List<String> copy(List<String> values) {
        return SkillManagementValueSupport.sortedDistinctCompactStrings(values);
    }
}
