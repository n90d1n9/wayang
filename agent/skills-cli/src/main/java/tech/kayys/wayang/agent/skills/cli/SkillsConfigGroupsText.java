package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigGroupSummary;

import java.io.PrintStream;

final class SkillsConfigGroupsText {

    private SkillsConfigGroupsText() {
    }

    static void render(SkillManagementRuntimeConfigCatalog catalog, PrintStream out) {
        out.printf(
                "runtime config groups: %d hints=%d%n",
                catalog.groups().size(),
                catalog.hintCount());
        catalog.groupSummaries().forEach(summary -> render(summary, out));
    }

    private static void render(
            SkillManagementRuntimeConfigGroupSummary summary,
            PrintStream out) {
        out.printf(
                "- %s: %s hints=%d%n",
                summary.name(),
                summary.label(),
                summary.hintCount());
    }
}
