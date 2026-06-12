package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage adapter boundary for Hermes-learned skill definitions and artifacts.
 */
public interface HermesLearnedSkillPersistenceAdapter {

    default String adapterId() {
        return HermesText.oneLineOr(getClass().getSimpleName(), "learned-skill-persistence");
    }

    default HermesSkillPersistenceTargetPlan targetPlan() {
        return HermesSkillPersistencePlan.from(null).targetPlan();
    }

    Uni<Optional<SkillDefinition>> find(String skillId);

    Uni<List<SkillDefinition>> listLearnedSkills();

    SkillValidation validate(SkillDefinition skill);

    Uni<SkillDefinition> create(SkillDefinition skill, SkillArtifact artifact);

    Uni<SkillDefinition> update(String skillId, SkillDefinition skill, SkillArtifact artifact);

    default Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("adapterId", adapterId());
        values.put("targetPlan", targetPlan().toMetadata());
        return Map.copyOf(values);
    }
}
