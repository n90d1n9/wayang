package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfigs;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceProfile;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceProfiles;

import java.util.Objects;

record SkillsPersistenceConfigSource(
        String source,
        String profile,
        boolean runtime,
        SkillManagementServiceConfig config) {

    private static final String SOURCE_DEFAULT = "default";
    private static final String SOURCE_PROFILE = "profile";
    private static final String SOURCE_RUNTIME = "runtime";

    SkillsPersistenceConfigSource {
        source = source == null || source.isBlank() ? SOURCE_DEFAULT : source.trim();
        profile = profile == null ? "" : profile.trim();
        config = Objects.requireNonNull(config, "config");
    }

    static SkillsPersistenceConfigSource resolve(
            String profileName,
            boolean runtimeConfig,
            SkillManagementServiceConfig defaultConfig) {
        boolean profileRequested = profileName != null && !profileName.isBlank();
        if (profileRequested && runtimeConfig) {
            throw new IllegalArgumentException(
                    "Choose only one skill persistence config source: --profile or --runtime.");
        }
        if (profileRequested) {
            SkillManagementServiceProfile profile = SkillManagementServiceProfiles.profile(profileName);
            return new SkillsPersistenceConfigSource(
                    SOURCE_PROFILE,
                    profile.label(),
                    false,
                    SkillManagementServiceProfiles.config(profile));
        }
        if (runtimeConfig) {
            return new SkillsPersistenceConfigSource(
                    SOURCE_RUNTIME,
                    "",
                    true,
                    SkillManagementServiceConfigs.fromRuntime());
        }
        return new SkillsPersistenceConfigSource(
                SOURCE_DEFAULT,
                "",
                false,
                defaultConfig == null ? SkillManagementServiceConfig.defaults() : defaultConfig);
    }
}
