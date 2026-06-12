package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only operational view of a skill artifact store.
 */
public record SkillArtifactStoreInspection(
        String name,
        String storeType,
        SkillArtifactStoreHealthStatus status,
        int artifactCount,
        List<String> artifactReferences,
        Map<String, Integer> kindCounts,
        String failure,
        List<SkillArtifactStoreInspection> children,
        SkillStoreCapabilities capabilities) {

    public SkillArtifactStoreInspection {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (storeType == null || storeType.isBlank()) {
            throw new IllegalArgumentException("storeType must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        artifactReferences = SkillStoreInspectionSupport.ids(artifactReferences);
        artifactCount = SkillStoreInspectionSupport.countAtLeast(artifactCount, artifactReferences.size());
        kindCounts = SkillStoreInspectionSupport.counts(kindCounts);
        failure = SkillStoreInspectionSupport.text(failure);
        children = SkillStoreInspectionSupport.children(children);
        capabilities = capabilities == null ? SkillStoreCapabilities.none() : capabilities;
    }

    public boolean ready() {
        return status == SkillArtifactStoreHealthStatus.READY;
    }

    public static SkillArtifactStoreInspection ready(
            String name,
            String storeType,
            List<SkillArtifactReference> artifacts,
            List<SkillArtifactStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        List<SkillArtifactReference> resolvedArtifacts = SkillManagementValueSupport.nonNullList(artifacts);
        return new SkillArtifactStoreInspection(
                name,
                storeType,
                SkillArtifactStoreHealthStatus.READY,
                resolvedArtifacts.size(),
                resolvedArtifacts.stream()
                        .map(SkillArtifactReference::qualifiedName)
                        .sorted()
                        .toList(),
                kindCounts(resolvedArtifacts),
                "",
                children,
                capabilities);
    }

    public static SkillArtifactStoreInspection unavailable(
            String name,
            String storeType,
            String failure,
            List<SkillArtifactStoreInspection> children,
            SkillStoreCapabilities capabilities) {
        return new SkillArtifactStoreInspection(
                name,
                storeType,
                SkillArtifactStoreHealthStatus.UNAVAILABLE,
                0,
                List.of(),
                Map.of(),
                failure,
                children,
                capabilities);
    }

    private static Map<String, Integer> kindCounts(List<SkillArtifactReference> artifacts) {
        return artifacts.stream()
                .filter(Objects::nonNull)
                .map(SkillArtifactReference::kind)
                .filter(Objects::nonNull)
                .map(SkillArtifactKind::label)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        label -> label,
                        label -> 1,
                        Integer::sum));
    }
}
