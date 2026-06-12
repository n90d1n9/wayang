package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminDeploymentPreflightReport;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.util.Objects;

/**
 * CLI-facing envelope for skill persistence status output.
 */
record SkillsPersistenceStatusReport(
        String source,
        String profile,
        boolean runtime,
        SkillManagementAdminPersistenceStrategy persistence,
        SkillManagementAdminDeploymentPreflightReport preflight,
        SkillsPersistenceConfigDiagnostics diagnostics) {

    SkillsPersistenceStatusReport(
            String source,
            String profile,
            boolean runtime,
            SkillManagementAdminPersistenceStrategy persistence) {
        this(source, profile, runtime, persistence, null, null);
    }

    SkillsPersistenceStatusReport(
            String source,
            String profile,
            boolean runtime,
            SkillManagementAdminPersistenceStrategy persistence,
            SkillManagementAdminDeploymentPreflightReport preflight) {
        this(source, profile, runtime, persistence, preflight, null);
    }

    SkillsPersistenceStatusReport {
        source = source == null || source.isBlank() ? "default" : source.trim();
        profile = profile == null ? "" : profile.trim();
        persistence = Objects.requireNonNull(persistence, "persistence");
    }

    boolean profiled() {
        return !profile.isBlank();
    }

    String sourceLabel() {
        return profiled() ? source + " (" + profile + ")" : source;
    }

    boolean preflightAvailable() {
        return preflight != null;
    }

    boolean diagnosticsAvailable() {
        return diagnostics != null;
    }
}
