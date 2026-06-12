package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceBackendCapabilitiesTest {

    @Test
    void databaseDefinitionRouteRequiresCredentialsAndSupportsDefinitions() {
        HermesSkillPersistenceBackendCapabilities capabilities =
                HermesSkillPersistenceBackendProfile.from(new HermesSkillPersistenceRoute(
                        "definitions",
                        "postgres",
                        "database",
                        10,
                        false))
                        .capabilities();

        assertThat(capabilities.readable()).isTrue();
        assertThat(capabilities.writable()).isTrue();
        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.requiresCredentials()).isTrue();
        assertThat(capabilities.supportsDefinitions()).isTrue();
        assertThat(capabilities.supportsArtifacts()).isFalse();
        assertThat(capabilities.toMetadata())
                .containsEntry("requiresCredentials", true)
                .containsEntry("supportsDefinitions", true);
    }

    @Test
    void cloudArtifactRouteSupportsArtifactsAsSupplementalStore() {
        HermesSkillPersistenceBackendCapabilities capabilities =
                HermesSkillPersistenceBackendProfile.from(new HermesSkillPersistenceRoute(
                        "cloud",
                        "s3",
                        "object-storage",
                        52,
                        false))
                        .capabilities();

        assertThat(capabilities.durable()).isTrue();
        assertThat(capabilities.requiresCredentials()).isTrue();
        assertThat(capabilities.supportsDefinitions()).isFalse();
        assertThat(capabilities.supportsArtifacts()).isTrue();
        assertThat(capabilities.supplementalCloud()).isTrue();
    }

    @Test
    void fileFallbackSupportsDefinitionsAndArtifactsWithoutCredentials() {
        HermesSkillPersistenceBackendCapabilities capabilities =
                HermesSkillPersistenceBackendProfile.from(new HermesSkillPersistenceRoute(
                        "fallback",
                        "file-system",
                        "file",
                        100,
                        true))
                        .capabilities();

        assertThat(capabilities.fallbackOnly()).isTrue();
        assertThat(capabilities.requiresCredentials()).isFalse();
        assertThat(capabilities.supportsDefinitions()).isTrue();
        assertThat(capabilities.supportsArtifacts()).isTrue();
    }
}
