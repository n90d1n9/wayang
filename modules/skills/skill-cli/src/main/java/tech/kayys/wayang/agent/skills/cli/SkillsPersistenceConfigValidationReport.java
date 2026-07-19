package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;

import java.util.List;

/**
 * CLI-facing validation envelope for resolved skill persistence config.
 */
record SkillsPersistenceConfigValidationReport(
        String source,
        String profile,
        boolean runtime,
        boolean valid,
        List<String> errors,
        SkillsPersistenceConfigValidationPolicyResult policy,
        List<String> warnings,
        SkillManagementAdminPersistenceStrategy persistence) {

    SkillsPersistenceConfigValidationReport {
        source = source == null || source.isBlank() ? "default" : source.trim();
        profile = profile == null ? "" : profile.trim();
        errors = errors == null ? List.of() : List.copyOf(errors);
        policy = java.util.Objects.requireNonNull(policy, "policy");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        persistence = java.util.Objects.requireNonNull(persistence, "persistence");
    }

    static SkillsPersistenceConfigValidationReport from(SkillsPersistenceConfigResolution resolution) {
        return from(resolution, SkillsPersistenceConfigValidationPolicy.defaults());
    }

    static SkillsPersistenceConfigValidationReport from(
            SkillsPersistenceConfigResolution resolution,
            SkillsPersistenceConfigValidationPolicy policy) {
        return new SkillsPersistenceConfigValidationReport(
                resolution.source(),
                resolution.profile(),
                resolution.runtime(),
                resolution.valid(),
                resolution.errors(),
                policy.evaluate(resolution),
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

    int policyErrorCount() {
        return policy.errorCount();
    }

    boolean passed() {
        return valid && policy.passed();
    }

    boolean requireDurable() {
        return policy.requireDurable();
    }

    List<String> policyErrors() {
        return policy.errors();
    }

    int warningCount() {
        return warnings.size();
    }
}
