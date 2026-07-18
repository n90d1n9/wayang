package tech.kayys.wayang.agent.skills.cli;

import java.util.List;

/**
 * Optional deployment policy gates for resolved skill persistence config.
 */
record SkillsPersistenceConfigValidationPolicy(boolean requireDurable) {

    private static final String FULLY_DURABLE_REQUIRED =
            "Fully durable skill persistence is required.";

    static SkillsPersistenceConfigValidationPolicy defaults() {
        return new SkillsPersistenceConfigValidationPolicy(false);
    }

    static SkillsPersistenceConfigValidationPolicy requiringDurable(boolean requireDurable) {
        return new SkillsPersistenceConfigValidationPolicy(requireDurable);
    }

    SkillsPersistenceConfigValidationPolicyResult evaluate(SkillsPersistenceConfigResolution resolution) {
        if (requireDurable && !resolution.persistence().fullyDurable()) {
            return new SkillsPersistenceConfigValidationPolicyResult(
                    requireDurable,
                    List.of(FULLY_DURABLE_REQUIRED));
        }
        return new SkillsPersistenceConfigValidationPolicyResult(requireDurable, List.of());
    }
}
