package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillLifecycleState;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Objects;

/**
 * Skill definition detail plus lifecycle state for CLI rendering.
 */
record SkillsDefinitionInfoReport(
        SkillDefinition skill,
        SkillLifecycleState lifecycleState) {

    SkillsDefinitionInfoReport {
        skill = Objects.requireNonNull(skill, "skill");
        lifecycleState = Objects.requireNonNull(lifecycleState, "lifecycleState");
    }
}
