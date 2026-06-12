package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;
import tech.kayys.wayang.agent.skills.management.SkillManagementObjectStorageProviderConfigAssessment;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceProfile;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceConfigWarningsTest {

    @Test
    void mergesProviderWarningsForRuntimeExternalProviderConfig() {
        SkillsPersistenceConfigSource source = source(
                "runtime",
                "",
                true,
                SkillManagementServiceProfiles.config(SkillManagementServiceProfile.OBJECT_STORAGE));

        List<String> warnings = SkillsPersistenceConfigWarnings.from(
                source,
                SkillManagementAdminViews.persistenceStrategy(source.config()),
                assessment("missing provider settings"));

        assertThat(warnings).containsExactly("missing provider settings");
    }

    @Test
    void ignoresProviderWarningsForProfileConfig() {
        SkillsPersistenceConfigSource source = source(
                "profile",
                "object-storage",
                false,
                SkillManagementServiceProfiles.config(SkillManagementServiceProfile.OBJECT_STORAGE));

        List<String> warnings = SkillsPersistenceConfigWarnings.from(
                source,
                SkillManagementAdminViews.persistenceStrategy(source.config()),
                assessment("missing provider settings"));

        assertThat(warnings).isEmpty();
    }

    @Test
    void ignoresProviderWarningsForRuntimeConfigWithoutExternalProvider() {
        SkillsPersistenceConfigSource source = source(
                "runtime",
                "",
                true,
                SkillManagementServiceConfig.defaults());

        List<String> warnings = SkillsPersistenceConfigWarnings.from(
                source,
                SkillManagementAdminViews.persistenceStrategy(source.config()),
                assessment("missing provider settings"));

        assertThat(warnings)
                .containsExactly(
                        "Disabled skill persistence roles: event-history",
                        "Ephemeral skill persistence roles: definition, lifecycle-state, artifact")
                .doesNotContain("missing provider settings");
    }

    private static SkillsPersistenceConfigSource source(
            String source,
            String profile,
            boolean runtime,
            SkillManagementServiceConfig config) {
        return new SkillsPersistenceConfigSource(source, profile, runtime, config);
    }

    private static SkillManagementObjectStorageProviderConfigAssessment assessment(String warning) {
        return new SkillManagementObjectStorageProviderConfigAssessment(List.of(), List.of(warning));
    }
}
