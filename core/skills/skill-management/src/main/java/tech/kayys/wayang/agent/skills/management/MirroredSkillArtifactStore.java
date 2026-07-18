package tech.kayys.wayang.agent.skills.management;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Artifact store that reads as primary/fallback and writes to both.
 */
public final class MirroredSkillArtifactStore implements SkillArtifactStore {

    private final SkillArtifactStore primary;
    private final SkillArtifactStore fallback;

    public MirroredSkillArtifactStore(SkillArtifactStore primary, SkillArtifactStore fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public SkillArtifactStore primary() {
        return primary;
    }

    public SkillArtifactStore fallback() {
        return fallback;
    }

    @Override
    public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
        return HybridSkillStoreSupport.primaryOrFallback(
                () -> primary.getArtifact(reference),
                () -> fallback.getArtifact(reference));
    }

    @Override
    public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
        SkillArtifactQuery resolved = query == null ? SkillArtifactQuery.all() : query;
        SkillArtifactQuery expanded = new SkillArtifactQuery(
                resolved.skillId(),
                resolved.kind(),
                resolved.name(),
                resolved.version(),
                SkillArtifactQuery.MAX_LIMIT);
        return HybridSkillStoreSupport.mergeFallbackThenPrimary(
                        () -> fallback.listArtifacts(expanded),
                        () -> primary.listArtifacts(expanded),
                        Function.identity())
                .stream()
                .sorted(Comparator.comparing(SkillArtifactReference::qualifiedName))
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public void putArtifact(SkillArtifact artifact) {
        primary.putArtifact(artifact);
        fallback.putArtifact(artifact);
    }

    @Override
    public boolean deleteArtifact(SkillArtifactReference reference) {
        return HybridSkillStoreSupport.removeFromBoth(
                () -> primary.deleteArtifact(reference),
                () -> fallback.deleteArtifact(reference));
    }
}
