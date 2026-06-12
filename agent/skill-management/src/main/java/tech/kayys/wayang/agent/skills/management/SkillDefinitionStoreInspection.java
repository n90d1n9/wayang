package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Read-only operational view of a skill definition store.
 */
public record SkillDefinitionStoreInspection(
        String name,
        String storeType,
        SkillDefinitionStoreHealthStatus status,
        int skillCount,
        List<String> skillIds,
        String failure,
        List<SkillDefinitionStoreInspection> children,
        SkillStoreCapabilities capabilities) {

    public SkillDefinitionStoreInspection(
            String name,
            String storeType,
            SkillDefinitionStoreHealthStatus status,
            int skillCount,
            List<String> skillIds,
            String failure,
            List<SkillDefinitionStoreInspection> children) {
        this(name, storeType, status, skillCount, skillIds, failure, children, SkillStoreCapabilities.definitionStore());
    }

    public SkillDefinitionStoreInspection {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (storeType == null || storeType.isBlank()) {
            throw new IllegalArgumentException("storeType must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        skillIds = SkillStoreInspectionSupport.ids(skillIds);
        skillCount = SkillStoreInspectionSupport.countAtLeast(skillCount, skillIds.size());
        failure = SkillStoreInspectionSupport.text(failure);
        children = SkillStoreInspectionSupport.children(children);
        capabilities = capabilities == null ? SkillStoreCapabilities.none() : capabilities;
    }

    public boolean ready() {
        return status == SkillDefinitionStoreHealthStatus.READY;
    }

    public static SkillDefinitionStoreInspection ready(
            String name,
            String storeType,
            List<String> skillIds,
            List<SkillDefinitionStoreInspection> children) {
        return ready(name, storeType, skillIds, children, SkillStoreCapabilities.definitionStore());
    }

    public static SkillDefinitionStoreInspection ready(
            String name,
            String storeType,
            List<String> skillIds,
            List<SkillDefinitionStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        List<String> resolvedSkillIds = SkillStoreInspectionSupport.ids(skillIds);
        return new SkillDefinitionStoreInspection(
                name,
                storeType,
                SkillDefinitionStoreHealthStatus.READY,
                resolvedSkillIds.size(),
                resolvedSkillIds,
                "",
                children,
                capabilities);
    }

    public static SkillDefinitionStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillDefinitionStoreInspection> children) {
        return unavailable(name, storeType, failure, children, SkillStoreCapabilities.definitionStore());
    }

    public static SkillDefinitionStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillDefinitionStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        return new SkillDefinitionStoreInspection(
                name,
                storeType,
                SkillDefinitionStoreHealthStatus.UNAVAILABLE,
                0,
                List.of(),
                failure,
                children,
                capabilities);
    }
}
