package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceBackendProfileTest {

    @Test
    void profilesDatabaseRouteAsAdapterBackend() {
        HermesSkillPersistenceBackendProfile profile = HermesSkillPersistenceBackendProfile.from(
                new HermesSkillPersistenceRoute("Definitions", "postgres", "database", 10, false));

        assertThat(profile)
                .returns("database", HermesSkillPersistenceBackendProfile::backendId)
                .returns(HermesSkillPersistenceRouteRoles.DEFINITIONS, HermesSkillPersistenceBackendProfile::routeRole)
                .returns("database", HermesSkillPersistenceBackendProfile::storageFamily)
                .returns(false, HermesSkillPersistenceBackendProfile::fallback);
        assertThat(profile.aliases()).contains("jdbc", "postgres", "mysql");
        assertThat(profile.capabilities().requiresCredentials()).isTrue();
        assertThat(profile.capabilities().supportsDefinitions()).isTrue();
        assertThat(profile.toMetadata())
                .containsEntry("backendId", "database")
                .containsKey("capabilities")
                .containsKey("storeDescriptor");
    }

    @Test
    void profilesCloudProviderByCanonicalStore() {
        HermesSkillPersistenceBackendProfile profile = HermesSkillPersistenceBackendProfile.from(
                new HermesSkillPersistenceRoute("artifacts", "Amazon S3", "object-storage", 20, false));

        assertThat(profile)
                .returns("s3", HermesSkillPersistenceBackendProfile::backendId)
                .returns("object-storage", HermesSkillPersistenceBackendProfile::storageFamily);
        assertThat(profile.capabilities().supportsArtifacts()).isTrue();
        assertThat(profile.aliases()).contains("object-storage", "s3-compatible", "rustfs");
    }

    @Test
    void profilesFallbackFileRoute() {
        HermesSkillPersistenceBackendProfile profile = HermesSkillPersistenceBackendProfile.from(
                new HermesSkillPersistenceRoute("fallback", "local-file", "file", 100, true));

        assertThat(profile)
                .returns("file-system", HermesSkillPersistenceBackendProfile::backendId)
                .returns("file-system", HermesSkillPersistenceBackendProfile::storageFamily)
                .returns(true, HermesSkillPersistenceBackendProfile::fallback);
        assertThat(profile.capabilities().fallbackOnly()).isTrue();
        assertThat(profile.roleIs("Fallback")).isTrue();
        assertThat(profile.storageFamilyIs("file system")).isTrue();
        assertThat(profile.storageFamilyIs("file-system")).isTrue();
    }
}
