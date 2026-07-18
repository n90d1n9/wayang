package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for inspecting one named persistence profile.
 */
record SkillsPersistenceProfileInspectRequest(
        String profileName,
        boolean includePreflight,
        boolean includeDiagnostics) {

    SkillsPersistenceProfileInspectRequest {
        profileName = profileName == null ? "" : profileName.trim();
    }

    static SkillsPersistenceProfileInspectRequest defaults() {
        return new SkillsPersistenceProfileInspectRequest("default", false, false);
    }

    static SkillsPersistenceProfileInspectRequest fromOptions(
            String profileName,
            boolean includePreflight,
            boolean includeDiagnostics) {
        return new SkillsPersistenceProfileInspectRequest(
                profileName,
                includePreflight,
                includeDiagnostics);
    }
}
