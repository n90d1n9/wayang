package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;

final class SkillsDefinitionCommandText {

    private SkillsDefinitionCommandText() {
    }

    static void renderRegistration(SkillsDefinitionRegistrationReport report, PrintStream out) {
        out.printf("Registered skill: %s%n", report.skill().id());
    }

    static void renderValidation(SkillsDefinitionValidationReport report, PrintStream out, PrintStream err) {
        if (report.valid()) {
            out.println("Skill definition is valid.");
            return;
        }
        report.errors().forEach(err::println);
    }
}
