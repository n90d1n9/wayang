package tech.kayys.wayang.agent.skills.management;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory artifact store for tests, harnesses, and lightweight local use.
 */
public final class InMemorySkillArtifactStore implements SkillArtifactStore {

    private final ConcurrentMap<SkillArtifactReference, SkillArtifact> artifacts = new ConcurrentHashMap<>();

    @Override
    public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
        return Optional.ofNullable(artifacts.get(requireReference(reference)));
    }

    @Override
    public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
        SkillArtifactQuery resolved = query == null ? SkillArtifactQuery.all() : query;
        return artifacts.keySet().stream()
                .filter(resolved::matches)
                .sorted(Comparator.comparing(SkillArtifactReference::qualifiedName))
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public void putArtifact(SkillArtifact artifact) {
        SkillArtifact stored = copy(Objects.requireNonNull(artifact, "artifact"));
        artifacts.put(stored.reference(), stored);
    }

    @Override
    public boolean deleteArtifact(SkillArtifactReference reference) {
        return artifacts.remove(requireReference(reference)) != null;
    }

    public void clear() {
        artifacts.clear();
    }

    private static SkillArtifactReference requireReference(SkillArtifactReference reference) {
        return Objects.requireNonNull(reference, "reference");
    }

    private static SkillArtifact copy(SkillArtifact artifact) {
        return new SkillArtifact(
                artifact.reference(),
                artifact.content(),
                artifact.contentType(),
                artifact.metadata());
    }
}
