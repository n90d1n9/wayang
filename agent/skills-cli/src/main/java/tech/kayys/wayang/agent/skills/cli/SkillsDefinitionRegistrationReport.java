package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Objects;

/**
 * Result of registering a CLI skill definition.
 */
record SkillsDefinitionRegistrationReport(SkillDefinition skill) {

    SkillsDefinitionRegistrationReport {
        skill = Objects.requireNonNull(skill, "skill");
    }
}
