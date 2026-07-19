package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;

final class SkillsPersistenceConfigResolveText {

    private SkillsPersistenceConfigResolveText() {
    }

    static void render(SkillsPersistenceConfigResolveReport report, PrintStream out) {
        out.printf("config source: %s%n", report.sourceLabel());
        out.printf("config valid: %s%n", report.valid());
        out.printf("errors: %d%n", report.errorCount());
        report.errors().forEach(error -> out.printf("- %s%n", error));
        out.println("resolved config:");
        SkillsPersistenceConfigDiagnosticsText.render(report.diagnostics(), out);
        out.printf("persistence strategy: %s%n", report.persistence().strategy());
        out.printf("fully durable: %s%n", report.persistence().fullyDurable());
        warnings(report, out);
    }

    private static void warnings(
            SkillsPersistenceConfigResolveReport report,
            PrintStream out) {
        if (report.warnings().isEmpty()) {
            out.println("warnings: -");
            return;
        }
        out.printf("warnings: %d%n", report.warningCount());
        report.warnings().forEach(warning -> out.printf("- %s%n", warning));
    }
}
