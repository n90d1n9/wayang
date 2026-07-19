package tech.kayys.wayang.agent.skills.management;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only inspector for configured skill lifecycle state stores.
 */
public final class SkillLifecycleStateStoreInspector {

    public SkillLifecycleStateStoreInspection inspect(SkillLifecycleStateStore store) {
        return inspect("lifecycle", store);
    }

    public SkillLifecycleStateStoreInspection inspect(String name, SkillLifecycleStateStore store) {
        SkillStoreInspectionSupport.require(store, "store");
        String storeType = SkillStoreInspectionSupport.storeType(store);
        SkillStoreCapabilities capabilities = SkillStoreInspectionSupport.lifecycleCapabilities(store);
        List<SkillLifecycleStateStoreInspection> children = children(store);
        try {
            Map<String, SkillLifecycleState> states = store.snapshot();
            List<String> skillIds = SkillStoreInspectionSupport.sortedNonBlankIds(
                    states.entrySet().stream()
                            .map(entry -> stateSkillId(entry.getKey(), entry.getValue())));
            return SkillLifecycleStateStoreInspection.ready(
                    name,
                    storeType,
                    skillIds,
                    statusCounts(states),
                    children,
                    capabilities);
        } catch (RuntimeException error) {
            return SkillLifecycleStateStoreInspection.unavailable(
                    name,
                    storeType,
                    SkillStoreInspectionSupport.errorMessage(error),
                    children,
                    capabilities);
        }
    }

    private List<SkillLifecycleStateStoreInspection> children(SkillLifecycleStateStore store) {
        if (store instanceof HybridSkillLifecycleStateStore hybrid) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    hybrid.primary(),
                    hybrid.fallback(),
                    this::inspect);
        }
        if (store instanceof MirroredSkillLifecycleStateStore mirrored) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    mirrored.primary(),
                    mirrored.fallback(),
                    this::inspect);
        }
        return List.of();
    }

    private Map<SkillLifecycleStatus, Integer> statusCounts(Map<String, SkillLifecycleState> states) {
        EnumMap<SkillLifecycleStatus, Integer> counts = new EnumMap<>(SkillLifecycleStatus.class);
        states.values().stream()
                .filter(Objects::nonNull)
                .map(SkillLifecycleState::status)
                .filter(Objects::nonNull)
                .forEach(status -> counts.merge(status, 1, Integer::sum));
        return counts;
    }

    private String stateSkillId(String key, SkillLifecycleState state) {
        if (state != null && state.skillId() != null && !state.skillId().isBlank()) {
            return state.skillId();
        }
        return key;
    }
}
