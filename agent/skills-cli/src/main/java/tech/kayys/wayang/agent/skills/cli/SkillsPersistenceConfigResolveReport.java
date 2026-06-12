package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.util.List;

/**
 * CLI-facing envelope for effective skill persistence config resolution.
 */
record SkillsPersistenceConfigResolveReport(
        String source,
        String profile,
        boolean runtime,
        boolean valid,
        List<String> errors,
        SkillsPersistenceConfigDiagnostics diagnostics,
        List<String> warnings,
        SkillManagementAdminPersistenceStrategy persistence) {

    SkillsPersistenceConfigResolveReport {
        source = source == null || source.isBlank() ? "default" : source.trim();
        profile = profile == null ? "" : profile.trim();
        errors = errors == null ? List.of() : List.copyOf(errors);
        diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        persistence = java.util.Objects.requireNonNull(persistence, "persistence");
    }

    static SkillsPersistenceConfigResolveReport from(SkillsPersistenceConfigResolution resolution) {
        return new SkillsPersistenceConfigResolveReport(
                resolution.source(),
                resolution.profile(),
                resolution.runtime(),
                resolution.valid(),
                resolution.errors(),
                resolution.diagnostics(),
                resolution.warnings(),
                resolution.persistence());
    }

    boolean profiled() {
        return !profile.isBlank();
    }

    String sourceLabel() {
        return profiled() ? source + " (" + profile + ")" : source;
    }

    int errorCount() {
        return errors.size();
    }

    int warningCount() {
        return warnings.size();
    }
}
