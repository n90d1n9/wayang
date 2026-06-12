package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;

final class SkillsDefinitionInfoCommandText {

    private SkillsDefinitionInfoCommandText() {
    }

    static void render(SkillsDefinitionInfoCommandReport report, PrintStream out, PrintStream err) {
        if (report.found()) {
            SkillsDefinitionInfoText.render(report.info().orElseThrow(), out);
            return;
        }
        err.printf("Skill not found: %s%n", report.skillId());
    }
}
