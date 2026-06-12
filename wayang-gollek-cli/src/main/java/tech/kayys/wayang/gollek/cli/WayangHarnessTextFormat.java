package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.HarnessCheck;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;

final class WayangHarnessTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangHarnessTextFormat() {
    }

    static String text(HarnessPlan plan) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang harness").append(NL);
        output.append("root: ").append(plan.workspace().rootPath()).append(NL);
        output.append("package managers: ").append(inline(plan.workspace().packageManagers())).append(NL);
        output.append("checks:").append(NL);
        if (plan.checks().isEmpty()) {
            output.append("- none").append(NL);
        }
        for (HarnessCheck check : plan.checks()) {
            output.append("- ").append(check.id())
                    .append(" [").append(check.optional() ? "optional" : "required").append("]")
                    .append(NL);
            output.append("  label: ").append(check.label()).append(NL);
            output.append("  cwd: ").append(check.workingDirectory()).append(NL);
            output.append("  command: ").append(check.commandLine()).append(NL);
            if (!check.reason().isBlank()) {
                output.append("  reason: ").append(check.reason()).append(NL);
            }
        }
        output.append("notes:").append(NL);
        if (plan.notes().isEmpty()) {
            output.append("- none").append(NL);
        }
        for (String note : plan.notes()) {
            output.append("- ").append(note).append(NL);
        }
        return output.toString();
    }

    private static String inline(Iterable<String> values) {
        StringBuilder output = new StringBuilder();
        for (String value : values) {
            if (!output.isEmpty()) {
                output.append(", ");
            }
            output.append(value);
        }
        return output.isEmpty() ? "none" : output.toString();
    }
}
