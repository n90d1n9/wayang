package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only operational view of a skill lifecycle state store.
 */
public record SkillLifecycleStateStoreInspection(
        String name,
        String storeType,
        SkillLifecycleStateStoreHealthStatus status,
        int stateCount,
        List<String> skillIds,
        Map<SkillLifecycleStatus, Integer> statusCounts,
        String failure,
        List<SkillLifecycleStateStoreInspection> children,
        SkillStoreCapabilities capabilities) {

    public SkillLifecycleStateStoreInspection(
            String name,
            String storeType,
            SkillLifecycleStateStoreHealthStatus status,
            int stateCount,
            List<String> skillIds,
            Map<SkillLifecycleStatus, Integer> statusCounts,
            String failure) {
        this(name, storeType, status, stateCount, skillIds, statusCounts, failure, List.of());
    }

    public SkillLifecycleStateStoreInspection(
            String name,
            String storeType,
            SkillLifecycleStateStoreHealthStatus status,
            int stateCount,
            List<String> skillIds,
            Map<SkillLifecycleStatus, Integer> statusCounts,
            String failure,
            List<SkillLifecycleStateStoreInspection> children) {
        this(
                name,
                storeType,
                status,
                stateCount,
                skillIds,
                statusCounts,
                failure,
                children,
                SkillStoreCapabilities.lifecycleStateStore());
    }

    public SkillLifecycleStateStoreInspection {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (storeType == null || storeType.isBlank()) {
            throw new IllegalArgumentException("storeType must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        skillIds = SkillStoreInspectionSupport.ids(skillIds);
        stateCount = SkillStoreInspectionSupport.countAtLeast(stateCount, skillIds.size());
        statusCounts = SkillStoreInspectionSupport.lifecycleStatusCounts(statusCounts);
        failure = SkillStoreInspectionSupport.text(failure);
        children = SkillStoreInspectionSupport.children(children);
        capabilities = capabilities == null ? SkillStoreCapabilities.none() : capabilities;
    }

    public boolean ready() {
        return status == SkillLifecycleStateStoreHealthStatus.READY;
    }

    public static SkillLifecycleStateStoreInspection ready(
            String name,
            String storeType,
            List<String> skillIds,
            Map<SkillLifecycleStatus, Integer> statusCounts,
            List<SkillLifecycleStateStoreInspection> children) {
        return ready(
                name,
                storeType,
                skillIds,
                statusCounts,
                children,
                SkillStoreCapabilities.lifecycleStateStore());
    }

    public static SkillLifecycleStateStoreInspection ready(
            String name,
            String storeType,
            List<String> skillIds,
            Map<SkillLifecycleStatus, Integer> statusCounts,
            List<SkillLifecycleStateStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        List<String> resolvedSkillIds = SkillStoreInspectionSupport.ids(skillIds);
        return new SkillLifecycleStateStoreInspection(
                name,
                storeType,
                SkillLifecycleStateStoreHealthStatus.READY,
                resolvedSkillIds.size(),
                resolvedSkillIds,
                statusCounts,
                "",
                children,
                capabilities);
    }

    public static SkillLifecycleStateStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillLifecycleStateStoreInspection> children) {
        return unavailable(name, storeType, failure, children, SkillStoreCapabilities.lifecycleStateStore());
    }

    public static SkillLifecycleStateStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillLifecycleStateStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        return new SkillLifecycleStateStoreInspection(
                name,
                storeType,
                SkillLifecycleStateStoreHealthStatus.UNAVAILABLE,
                0,
                List.of(),
                Map.of(),
                failure,
                children,
                capabilities);
    }

}
