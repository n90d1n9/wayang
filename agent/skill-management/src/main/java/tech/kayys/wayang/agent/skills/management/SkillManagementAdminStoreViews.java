package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Maps skill-management store inspections to stable admin DTOs.
 */
final class SkillManagementAdminStoreViews {

    private SkillManagementAdminStoreViews() {
    }

    static SkillManagementAdminStoreStatus definitionStore(SkillDefinitionStoreInspection inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return storeStatus(
                inspection.name(),
                inspection.storeType(),
                inspection.status().name(),
                inspection.ready(),
                inspection.skillCount(),
                inspection.skillIds(),
                Map.of(),
                inspection.failure(),
                inspection.children(),
                SkillManagementAdminStoreViews::definitionStore,
                inspection.capabilities());
    }

    static SkillManagementAdminStoreStatus lifecycleStore(SkillLifecycleStateStoreInspection inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return storeStatus(
                inspection.name(),
                inspection.storeType(),
                inspection.status().name(),
                inspection.ready(),
                inspection.stateCount(),
                inspection.skillIds(),
                lifecycleStatusCounts(inspection),
                inspection.failure(),
                inspection.children(),
                SkillManagementAdminStoreViews::lifecycleStore,
                inspection.capabilities());
    }

    static SkillManagementAdminStoreStatus eventStore(SkillManagementEventStoreInspection inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return storeStatus(
                inspection.name(),
                inspection.storeType(),
                inspection.status().name(),
                inspection.ready(),
                inspection.matchedEvents(),
                inspection.summary().skillCounts().keySet().stream().toList(),
                inspection.summary().operationCounts(),
                inspection.failure(),
                inspection.children(),
                SkillManagementAdminStoreViews::eventStore,
                inspection.capabilities());
    }

    static SkillManagementAdminStoreStatus artifactStore(SkillArtifactStoreInspection inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return storeStatus(
                inspection.name(),
                inspection.storeType(),
                inspection.status().name(),
                inspection.ready(),
                inspection.artifactCount(),
                inspection.artifactReferences(),
                inspection.kindCounts(),
                inspection.failure(),
                inspection.children(),
                SkillManagementAdminStoreViews::artifactStore,
                inspection.capabilities());
    }

    private static Map<String, Integer> lifecycleStatusCounts(SkillLifecycleStateStoreInspection inspection) {
        return inspection.statusCounts().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue));
    }

    private static <T> SkillManagementAdminStoreStatus storeStatus(
            String name,
            String storeType,
            String status,
            boolean ready,
            int itemCount,
            List<String> itemIds,
            Map<String, Integer> statusCounts,
            String failure,
            List<T> children,
            Function<T, SkillManagementAdminStoreStatus> childMapper,
            SkillStoreCapabilities capabilities) {
        return new SkillManagementAdminStoreStatus(
                name,
                storeType,
                status,
                ready,
                itemCount,
                itemIds,
                statusCounts,
                failure,
                (children == null ? List.<T>of() : children).stream()
                        .map(childMapper)
                        .toList(),
                capabilities == null ? List.of() : capabilities.names());
    }
}
