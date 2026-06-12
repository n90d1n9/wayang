package tech.kayys.wayang.agent.skills.management;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutation-free lifecycle reconciliation plan derived from definition and state ids.
 */
record SkillLifecycleStateReconcilePlan(
        List<String> definitionSkillIds,
        List<String> persistedStateSkillIds,
        List<String> missingStateSkillIds,
        List<String> orphanedStateSkillIds) {

    static SkillLifecycleStateReconcilePlan from(
            List<String> definitionSkillIds,
            List<String> persistedStateSkillIds) {
        List<String> definitions = ids(definitionSkillIds);
        List<String> persistedStates = ids(persistedStateSkillIds);
        return new SkillLifecycleStateReconcilePlan(
                definitions,
                persistedStates,
                difference(definitions, persistedStates),
                difference(persistedStates, definitions));
    }

    SkillLifecycleStateReconcileResult result(List<String> createdStateSkillIds, List<String> removedStateSkillIds) {
        return new SkillLifecycleStateReconcileResult(
                definitionSkillIds,
                persistedStateSkillIds,
                missingStateSkillIds,
                orphanedStateSkillIds,
                createdStateSkillIds,
                removedStateSkillIds);
    }

    private static List<String> ids(List<String> values) {
        return SkillManagementValueSupport.sortedDistinctCompactStrings(values);
    }

    private static List<String> difference(List<String> left, List<String> right) {
        Set<String> rightValues = new HashSet<>(right);
        return left.stream()
                .filter(value -> !rightValues.contains(value))
                .toList();
    }
}
