package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Read-only inspector for configured skill artifact stores.
 */
public final class SkillArtifactStoreInspector {

    public SkillArtifactStoreInspection inspect(SkillArtifactStore store) {
        return inspect("artifacts", store);
    }

    public SkillArtifactStoreInspection inspect(String name, SkillArtifactStore store) {
        SkillStoreInspectionSupport.require(store, "store");
        String storeType = SkillStoreInspectionSupport.storeType(store);
        SkillStoreCapabilities capabilities = SkillStoreInspectionSupport.artifactCapabilities(store);
        List<SkillArtifactStoreInspection> children = children(store);
        try {
            List<SkillArtifactReference> artifacts = store.listArtifacts(SkillArtifactQuery.all()).stream()
                    .filter(java.util.Objects::nonNull)
                    .sorted(java.util.Comparator.comparing(SkillArtifactReference::qualifiedName))
                    .toList();
            return SkillArtifactStoreInspection.ready(name, storeType, artifacts, children, capabilities);
        } catch (RuntimeException error) {
            return SkillArtifactStoreInspection.unavailable(
                    name,
                    storeType,
                    SkillStoreInspectionSupport.errorMessage(error),
                    children,
                    capabilities);
        }
    }

    private List<SkillArtifactStoreInspection> children(SkillArtifactStore store) {
        if (store instanceof HybridSkillArtifactStore hybrid) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    hybrid.primary(),
                    hybrid.fallback(),
                    this::inspect);
        }
        if (store instanceof MirroredSkillArtifactStore mirrored) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    mirrored.primary(),
                    mirrored.fallback(),
                    this::inspect);
        }
        return List.of();
    }
}
