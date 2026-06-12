package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for resolving effective skill persistence config.
 */
record SkillsPersistenceConfigResolveRequest(
        String profileName,
        boolean runtimeConfig) {

    SkillsPersistenceConfigResolveRequest {
        profileName = profileName == null ? "" : profileName.trim();
    }

    static SkillsPersistenceConfigResolveRequest defaults() {
        return new SkillsPersistenceConfigResolveRequest("", false);
    }

    static SkillsPersistenceConfigResolveRequest fromOptions(String profileName, boolean runtimeConfig) {
        return new SkillsPersistenceConfigResolveRequest(profileName, runtimeConfig);
    }
}
