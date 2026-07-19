package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads skill artifacts from the configured artifact store.
 */
final class SkillManagementArtifactReader {

    private final SkillArtifactStore artifactStore;

    SkillManagementArtifactReader(SkillArtifactStore artifactStore) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
    }

    Optional<SkillArtifact> get(SkillArtifactReference reference) {
        return artifactStore.getArtifact(reference);
    }

    List<SkillArtifactReference> list(SkillArtifactQuery query) {
        return artifactStore.listArtifacts(query);
    }

    List<SkillArtifactReference> list(String skillId) {
        return artifactStore.listArtifacts(skillId);
    }
}
