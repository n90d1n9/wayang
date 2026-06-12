package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.io.PrintStream;

final class SkillsDefinitionListText {

    private SkillsDefinitionListText() {
    }

    static void render(SkillsDefinitionListReport report, PrintStream out) {
        if (report.empty()) {
            out.println("No skills registered.");
            return;
        }
        report.skills().forEach(skill -> render(skill, out));
    }

    private static void render(SkillDefinition skill, PrintStream out) {
        out.printf("%s\t%s\t%s%n", skill.id(), skill.category(), skill.name());
    }
}
