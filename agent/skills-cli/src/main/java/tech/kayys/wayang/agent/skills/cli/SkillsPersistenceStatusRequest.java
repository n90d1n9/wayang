package tech.kayys.wayang.agent.skills.cli;

record SkillsPersistenceStatusRequest(
        String profileName,
        boolean runtimeConfig,
        boolean includePreflight,
        boolean includeDiagnostics) {

    SkillsPersistenceStatusRequest {
        profileName = profileName == null ? "" : profileName.trim();
    }

    static SkillsPersistenceStatusRequest defaults() {
        return new SkillsPersistenceStatusRequest("", false, false, false);
    }

    static SkillsPersistenceStatusRequest fromOptions(
            String profileName,
            boolean runtimeConfig,
            boolean includePreflight,
            boolean includeDiagnostics) {
        return new SkillsPersistenceStatusRequest(
                profileName,
                runtimeConfig,
                includePreflight,
                includeDiagnostics);
    }
}
