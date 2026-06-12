package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for resolving and validating skill persistence config.
 */
record SkillsPersistenceConfigValidationRequest(
        String profileName,
        boolean runtimeConfig,
        SkillsPersistenceConfigValidationPolicy policy) {

    SkillsPersistenceConfigValidationRequest {
        profileName = profileName == null ? "" : profileName.trim();
        policy = policy == null ? SkillsPersistenceConfigValidationPolicy.defaults() : policy;
    }

    static SkillsPersistenceConfigValidationRequest defaults() {
        return new SkillsPersistenceConfigValidationRequest(
                "",
                false,
                SkillsPersistenceConfigValidationPolicy.defaults());
    }

    static SkillsPersistenceConfigValidationRequest fromOptions(
            String profileName,
            boolean runtimeConfig,
            boolean requireDurable) {
        return new SkillsPersistenceConfigValidationRequest(
                profileName,
                runtimeConfig,
                SkillsPersistenceConfigValidationPolicy.requiringDurable(requireDurable));
    }
}
