package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementServiceProfilesTest {

    @Test
    void expandsObjectStorageProfileWithTunableOptions() {
        SkillManagementServiceProfileOptions options = SkillManagementServiceProfileOptions.defaults()
                .withObjectPrefix("tenants/acme/skills/")
                .withMaxEvents(125)
                .withLifecycleStateReconcileOptions(SkillLifecycleStateReconcileOptions.createMissing());

        SkillManagementServiceConfig config = SkillManagementServiceProfiles.config(
                SkillManagementServiceProfile.OBJECT_STORAGE,
                options);

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().objectPrefix()).isEqualTo("tenants/acme/skills/definitions");
        assertThat(config.lifecycleStateStore().kind())
                .isEqualTo(SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.lifecycleStateStore().objectPrefix()).isEqualTo("tenants/acme/skills/lifecycle");
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.eventStore().objectPrefix()).isEqualTo("tenants/acme/skills/events");
        assertThat(config.eventStore().maxEvents()).isEqualTo(125);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.artifactStore().objectPrefix()).isEqualTo("tenants/acme/skills/artifacts");
        assertThat(config.lifecycleStateReconcileOptions())
                .isEqualTo(SkillLifecycleStateReconcileOptions.createMissing());
    }

    @Test
    void expandsMirroredObjectFileProfile() {
        SkillManagementServiceConfig config = SkillManagementServiceProfiles.config(
                SkillManagementServiceProfile.MIRRORED_OBJECT_FILE,
                SkillManagementServiceProfileOptions.defaults()
                        .withBaseDirectory(Path.of("/var/lib/wayang/skills"))
                        .withObjectPrefix("prod/skills"));

        assertThat(config.definitionStore().kind()).isEqualTo(SkillDefinitionStoreConfig.Kind.MIRRORED);
        assertThat(config.definitionStore().primary().kind())
                .isEqualTo(SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE);
        assertThat(config.definitionStore().fallback().directory())
                .isEqualTo(Path.of("/var/lib/wayang/skills/definitions"));
        assertThat(config.lifecycleStateStore().kind()).isEqualTo(SkillLifecycleStateStoreConfig.Kind.MIRRORED);
        assertThat(config.eventStore().kind()).isEqualTo(SkillManagementEventStoreConfig.Kind.MIRRORED);
        assertThat(config.artifactStore().kind()).isEqualTo(SkillArtifactStoreConfig.Kind.MIRRORED);
    }

    @Test
    void resolvesProfileAliases() {
        assertThat(SkillManagementServiceProfiles.profile("rustfs"))
                .isEqualTo(SkillManagementServiceProfile.OBJECT_STORAGE);
        assertThat(SkillManagementServiceProfiles.profile("db"))
                .isEqualTo(SkillManagementServiceProfile.JDBC);
        assertThat(SkillManagementServiceProfiles.profile("mirror"))
                .isEqualTo(SkillManagementServiceProfile.MIRRORED_OBJECT_FILE);
        assertThatThrownBy(() -> SkillManagementServiceProfiles.profile("unknown-profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown skill-management service profile: unknown-profile");
    }

    @Test
    void exposesProfileDescriptorsForDiscovery() {
        assertThat(SkillManagementServiceProfiles.profiles())
                .extracting(SkillManagementServiceProfileDescriptor::label)
                .containsExactly(
                        "default",
                        "local-filesystem",
                        "object-storage",
                        "jdbc",
                        "hybrid-object-file",
                        "mirrored-object-file");

        assertThat(SkillManagementServiceProfiles.profileDescriptor("s3"))
                .satisfies(descriptor -> {
                    assertThat(descriptor.profile()).isEqualTo(SkillManagementServiceProfile.OBJECT_STORAGE);
                    assertThat(descriptor.aliases()).contains("rustfs", "cloud-storage");
                    assertThat(descriptor.description()).contains("S3/RustFS-compatible");
                });
    }
}
