package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for dynamic skill artifacts.
 */
public interface SkillArtifactStore {

    Optional<SkillArtifact> getArtifact(SkillArtifactReference reference);

    List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query);

    void putArtifact(SkillArtifact artifact);

    boolean deleteArtifact(SkillArtifactReference reference);

    default List<SkillArtifactReference> listArtifacts() {
        return listArtifacts(SkillArtifactQuery.all());
    }

    default List<SkillArtifactReference> listArtifacts(String skillId) {
        return listArtifacts(SkillArtifactQuery.forSkill(skillId));
    }
}
