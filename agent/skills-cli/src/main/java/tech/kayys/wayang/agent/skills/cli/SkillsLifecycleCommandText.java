package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;

final class SkillsLifecycleCommandText {

    private SkillsLifecycleCommandText() {
    }

    static void render(SkillsLifecycleCommandReport report, PrintStream out) {
        out.printf(
                "%s skill: %s (%s)%n",
                report.action().label(),
                report.state().skillId(),
                report.state().status());
    }
}
