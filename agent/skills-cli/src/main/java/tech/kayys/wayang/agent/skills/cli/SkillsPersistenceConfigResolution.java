package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Shared resolved persistence config projection for CLI config commands.
 */
record SkillsPersistenceConfigResolution(
        String source,
        String profile,
        boolean runtime,
        boolean valid,
        List<String> errors,
        SkillsPersistenceConfigDiagnostics diagnostics,
        List<String> warnings,
        SkillManagementAdminPersistenceStrategy persistence) {

    SkillsPersistenceConfigResolution {
        source = source == null || source.isBlank() ? "default" : source.trim();
        profile = profile == null ? "" : profile.trim();
        errors = errors == null ? List.of() : List.copyOf(errors);
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        persistence = Objects.requireNonNull(persistence, "persistence");
        warnings = warnings == null ? persistence.warnings() : List.copyOf(warnings);
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
