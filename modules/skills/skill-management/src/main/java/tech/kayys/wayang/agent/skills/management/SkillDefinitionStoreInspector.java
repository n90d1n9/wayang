package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;

/**
 * Read-only inspector for configured skill definition stores.
 */
public final class SkillDefinitionStoreInspector {

    public SkillDefinitionStoreInspection inspect(SkillDefinitionStore store) {
        return inspect("store", store);
    }

    public SkillDefinitionStoreInspection inspect(String name, SkillDefinitionStore store) {
        SkillStoreInspectionSupport.require(store, "store");
        String storeType = SkillStoreInspectionSupport.storeType(store);
        SkillStoreCapabilities capabilities = SkillStoreInspectionSupport.definitionCapabilities(store);
        List<SkillDefinitionStoreInspection> children = children(store);
        try {
            List<String> skillIds = SkillStoreInspectionSupport.sortedNonBlankIds(
                    store.listSkills().stream()
                            .filter(skill -> skill != null)
                            .map(SkillDefinition::id));
            return SkillDefinitionStoreInspection.ready(name, storeType, skillIds, children, capabilities);
        } catch (RuntimeException error) {
            return SkillDefinitionStoreInspection.unavailable(
                    name,
                    storeType,
                    SkillStoreInspectionSupport.errorMessage(error),
                    children,
                    capabilities);
        }
    }

    private List<SkillDefinitionStoreInspection> children(SkillDefinitionStore store) {
        if (store instanceof HybridSkillDefinitionStore hybrid) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    hybrid.primary(),
                    hybrid.fallback(),
                    this::inspect);
        }
        if (store instanceof MirroredSkillDefinitionStore mirrored) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    mirrored.primary(),
                    mirrored.fallback(),
                    this::inspect);
        }
        return List.of();
    }
}
