package tech.kayys.wayang.agent.skills.cli;

import java.util.List;

/**
 * Result of applying deployment policy gates to resolved skill persistence config.
 */
record SkillsPersistenceConfigValidationPolicyResult(
        boolean requireDurable,
        List<String> errors) {

    SkillsPersistenceConfigValidationPolicyResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    int errorCount() {
        return errors.size();
    }

    boolean passed() {
        return errors.isEmpty();
    }
}
