package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigCatalog;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigGroup;
import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigHint;

import java.io.PrintStream;

final class SkillsConfigExplainText {

    private SkillsConfigExplainText() {
    }

    static void render(SkillManagementRuntimeConfigCatalog catalog, PrintStream out) {
        out.printf(
                "runtime config hints: groups=%d hints=%d%n",
                catalog.groups().size(),
                catalog.hintCount());
        catalog.groups().forEach(group -> renderGroup(group, out));
    }

    private static void renderGroup(SkillManagementRuntimeConfigGroup group, PrintStream out) {
        out.printf("%s:%n", group.label());
        group.hints().forEach(hint -> renderHint(hint, out));
    }

    private static void renderHint(SkillManagementRuntimeConfigHint hint, PrintStream out) {
        out.printf("- %s: %s%n", hint.name(), hint.description());
        if (!hint.properties().isEmpty()) {
            out.printf("  properties: %s%n", String.join(",", hint.properties()));
        }
        if (!hint.environment().isEmpty()) {
            out.printf("  environment: %s%n", String.join(",", hint.environment()));
        }
        if (hint.hasDefaultValue()) {
            out.printf("  default: %s%n", hint.defaultValue());
        }
        hint.notes().forEach(note -> out.printf("  note: %s%n", note));
    }
}
