package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;

/**
 * Resolved skill definition list for CLI rendering.
 */
record SkillsDefinitionListReport(List<SkillDefinition> skills) {

    SkillsDefinitionListReport {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    boolean empty() {
        return skills.isEmpty();
    }
}
