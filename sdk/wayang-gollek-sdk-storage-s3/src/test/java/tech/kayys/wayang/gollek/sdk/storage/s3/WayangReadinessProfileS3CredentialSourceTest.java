package tech.kayys.wayang.gollek.sdk.storage.s3;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.storage.provider.s3.S3StorageConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessProfileS3CredentialSourceTest {

    @Test
    void environmentSourceUsesReadinessAliasesAndRedactsSecretValue() {
        WayangReadinessProfileS3CredentialSource source =
                WayangReadinessProfileS3CredentialSource.fromEnvironment(
                        "tenant-a",
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_ENV,
                                "tenant-access",
                                WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_ENV,
                                "very-sensitive-value"));

        assertThat(source.available()).isTrue();
        assertThat(source.credentials()).get()
                .extracting(WayangReadinessProfileS3Credentials::accessKeyId)
                .isEqualTo("tenant-access");
        assertThat(source.toMap())
                .containsEntry("credentialsId", "tenant-a")
                .containsEntry("sourceType", "environment")
                .containsEntry("available", true)
                .containsEntry("accessKeyIdKey",
                        WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_ENV)
                .containsEntry("secretAccessKeyKey",
                        WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_ENV);
        assertThat(source.toMap()).doesNotContainValue("very-sensitive-value");
        assertThat(source.toString()).doesNotContain("very-sensitive-value");
    }

    @Test
    void environmentSourceFallsBackToStorageS3Aliases() {
        WayangReadinessProfileS3CredentialSource source =
                WayangReadinessProfileS3CredentialSource.fromEnvironment(
                        Map.of(
                                S3StorageConfig.ACCESS_KEY_ID_ENV, "storage-access",
                                S3StorageConfig.SECRET_ACCESS_KEY_ENV, "storage-secret"));

        assertThat(source.credentialsId())
                .isEqualTo(WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID);
        assertThat(source.available()).isTrue();
        assertThat(source.credentials()).get()
                .extracting(WayangReadinessProfileS3Credentials::accessKeyId)
                .isEqualTo("storage-access");
        assertThat(source.toMap())
                .containsEntry("accessKeyIdKey", S3StorageConfig.ACCESS_KEY_ID_ENV)
                .containsEntry("secretAccessKeyKey", S3StorageConfig.SECRET_ACCESS_KEY_ENV);
    }

    @Test
    void mapSourceSupportsReadableAliasesAndIncompleteDiagnostics() {
        WayangReadinessProfileS3CredentialSource source =
                WayangReadinessProfileS3CredentialSource.fromMap(
                        "rustfs",
                        Map.of("accessKeyId", "rustfs-access"));

        assertThat(source.available()).isFalse();
        assertThat(source.credentials()).isEmpty();
        assertThat(source.toMap())
                .containsEntry("credentialsId", "rustfs")
                .containsEntry("accessKeyIdConfigured", true)
                .containsEntry("secretAccessKeyConfigured", false)
                .containsEntry("accessKeyIdKey", "accessKeyId")
                .containsEntry("secretAccessKeyKey", "")
                .containsEntry("message",
                        "S3 readiness profile credential source is missing secret access key.");
    }

    @Test
    void registryBuildsFromAvailableSourcesAndKeepsRedactedDiagnostics() {
        WayangReadinessProfileS3CredentialSource rustfs =
                WayangReadinessProfileS3CredentialSource.fromMap(
                        "rustfs",
                        Map.of(
                                "accessKeyId", "rustfs-access",
                                "secretAccessKey", "rustfs-secret"));
        WayangReadinessProfileS3CredentialSource minio =
                WayangReadinessProfileS3CredentialSource.fromMap(
                        "minio",
                        Map.of("accessKeyId", "minio-access"));

        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.fromSources(List.of(rustfs, minio));
        WayangReadinessProfileS3CredentialsResolution report = registry.resolveReport(
                WayangObjectStorageConfig.fromMap(Map.of(
                        "provider", "rustfs",
                        "bucket", "wayang",
                        "objectKey", "profiles/default.properties")));

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.find("rustfs")).isPresent();
        assertThat(registry.find("minio")).isEmpty();
        assertThat(report.available()).isTrue();
        assertThat(report.selectedCredentialsId()).isEqualTo("rustfs");
        assertThat(registry.credentialSourceDiagnostics())
                .hasSize(2)
                .anySatisfy(values -> assertThat(values)
                        .containsEntry("credentialsId", "rustfs")
                        .containsEntry("available", true))
                .anySatisfy(values -> assertThat(values)
                        .containsEntry("credentialsId", "minio")
                        .containsEntry("available", false));
        assertThat(registry.credentialSourceDiagnostics().toString())
                .doesNotContain("rustfs-secret");
    }
}
