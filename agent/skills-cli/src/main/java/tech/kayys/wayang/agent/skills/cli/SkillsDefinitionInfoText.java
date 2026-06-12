package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.io.PrintStream;

final class SkillsDefinitionInfoText {

    private SkillsDefinitionInfoText() {
    }

    static void render(SkillsDefinitionInfoReport report, PrintStream out) {
        SkillDefinition skill = report.skill();
        out.printf("id: %s%n", skill.id());
        out.printf("name: %s%n", skill.name());
        out.printf("category: %s%n", skill.category());
        out.printf("status: %s%n", report.lifecycleState().status());
        out.printf("description: %s%n", nullToEmpty(skill.description()));
        out.printf("tools: %s%n", skill.tools().isEmpty() ? "-" : String.join(",", skill.tools()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
