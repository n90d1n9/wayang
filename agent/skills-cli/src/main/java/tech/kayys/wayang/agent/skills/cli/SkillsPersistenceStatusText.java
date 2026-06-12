package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminDeploymentPreflightReport;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceRole;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.io.PrintStream;

final class SkillsPersistenceStatusText {

    private SkillsPersistenceStatusText() {
    }

    static void render(SkillsPersistenceStatusReport report, PrintStream out) {
        out.printf("config source: %s%n", report.sourceLabel());
        diagnostics(report, out);
        strategy(report.persistence(), out);
        preflight(report, out);
    }

    private static void diagnostics(
            SkillsPersistenceStatusReport report,
            PrintStream out) {
        if (!report.diagnosticsAvailable()) {
            return;
        }
        out.println("config diagnostics:");
        SkillsPersistenceConfigDiagnosticsText.render(report.diagnostics(), out);
    }

    private static void strategy(
            SkillManagementAdminPersistenceStrategy strategy,
            PrintStream out) {
        out.printf("persistence strategy: %s%n", strategy.strategy());
        out.printf("fully durable: %s%n", strategy.fullyDurable());
        out.printf(
                "roles: %d durable=%d ephemeral=%d disabled=%d custom=%d%n",
                strategy.roleCount(),
                strategy.durableRoleCount(),
                strategy.ephemeralRoleCount(),
                strategy.disabledRoleCount(),
                strategy.customRoleCount());
        out.printf(
                "providers: external=%s composite=%s mirrored=%s durable-fallback=%s%n",
                strategy.hasExternalProvider(),
                strategy.hasCompositeProvider(),
                strategy.hasMirroredProvider(),
                strategy.hasDurableFallback());
        warnings(strategy, out);
        out.println("stores:");
        strategy.roles().forEach(role -> role(role, "", out));
    }

    private static void preflight(
            SkillsPersistenceStatusReport report,
            PrintStream out) {
        if (!report.preflightAvailable()) {
            return;
        }
        SkillManagementAdminDeploymentPreflightReport preflight = report.preflight();
        out.printf(
                "preflight: ready=%s deployable=%s errors=%d%n",
                preflight.ready(),
                preflight.deployable(),
                preflight.errorCount());
        if (!preflight.message().isBlank()) {
            out.printf("preflight message: %s%n", preflight.message());
        }
    }

    private static void warnings(
            SkillManagementAdminPersistenceStrategy strategy,
            PrintStream out) {
        if (strategy.warnings().isEmpty()) {
            out.println("warnings: -");
            return;
        }
        out.println("warnings:");
        strategy.warnings().forEach(warning -> out.printf("- %s%n", warning));
    }

    private static void role(
            SkillManagementAdminPersistenceRole role,
            String indent,
            PrintStream out) {
        out.printf(
                "%s- %s: provider=%s class=%s strategy=%s durable=%s fallback=%s capabilities=%s%n",
                indent,
                role.role(),
                role.provider(),
                role.persistenceClass(),
                role.strategy(),
                role.durable(),
                role.durableFallback(),
                role.capabilities().isEmpty() ? "-" : String.join(",", role.capabilities()));
        role.children().forEach(child -> role(child, indent + "  ", out));
    }
}
