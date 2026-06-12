package tech.kayys.wayang.gollek.sdk.storage.s3;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceReader;
import tech.kayys.wayang.storage.provider.s3.S3ObjectStorageService;
import tech.kayys.wayang.storage.provider.s3.S3StorageConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangReadinessProfileS3ObjectStorageReaderFactoryTest {

    @Test
    void mapsAwsS3ReadinessProfileConfigToS3StorageConfig() {
        S3StorageConfig config = WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                WayangObjectStorageConfig.s3(
                        "",
                        "wayang-readiness",
                        "ap-southeast-3",
                        "profiles/default.properties"),
                WayangReadinessProfileS3Credentials.of("access", "secret"));

        assertThat(config.accessKeyId()).isEqualTo("access");
        assertThat(config.secretAccessKey()).isEqualTo("secret");
        assertThat(config.bucketName()).isEqualTo("wayang-readiness");
        assertThat(config.region()).isEqualTo("ap-southeast-3");
        assertThat(config.endpoint()).isEmpty();
        assertThat(config.pathPrefix()).isEmpty();
        assertThat(config.pathStyleAccess()).isFalse();
    }

    @Test
    void mapsRustfsReadinessProfileConfigToPathStyleS3StorageConfig() {
        S3StorageConfig config = WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"),
                WayangReadinessProfileS3Credentials.of("access", "secret"));

        assertThat(config.bucketName()).isEqualTo("wayang");
        assertThat(config.region()).isEqualTo(WayangReadinessProfileS3ObjectStorageReaderFactory.DEFAULT_REGION);
        assertThat(config.endpoint()).isEqualTo("http://localhost:9000");
        assertThat(config.pathPrefix()).isEmpty();
        assertThat(config.pathStyleAccess()).isTrue();
    }

    @Test
    void createsInitializedServiceAndReaderWithoutLiveNetworkCall() {
        WayangObjectStorageConfig objectStorage = WayangObjectStorageConfig.rustfs(
                "http://localhost:9000",
                "wayang",
                "profiles/default.properties");
        WayangReadinessProfileS3Credentials credentials =
                WayangReadinessProfileS3Credentials.of("access", "secret");

        S3ObjectStorageService service =
                WayangReadinessProfileS3ObjectStorageReaderFactory.service(objectStorage, credentials);
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileS3ObjectStorageReaderFactory.reader(objectStorage, credentials);

        assertThat(service).isNotNull();
        assertThat(reader).isNotNull();
    }

    @Test
    void credentialRegistryPrefersCredentialsRefOverProvider() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.builder()
                        .register("rustfs", WayangReadinessProfileS3Credentials.of("provider-access", "provider-secret"))
                        .register("tenant-a-readiness", WayangReadinessProfileS3Credentials.of(
                                "tenant-access",
                                "tenant-secret"))
                        .build();
        WayangObjectStorageConfig config = new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "tenant-a-readiness");

        S3StorageConfig storage = WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                config,
                registry.resolve(config).orElseThrow());
        WayangReadinessProfileS3CredentialsResolution report = registry.resolveReport(config);

        assertThat(storage.accessKeyId()).isEqualTo("tenant-access");
        assertThat(report.available()).isTrue();
        assertThat(report.selectedCredentialsId()).isEqualTo("tenant-a-readiness");
        assertThat(report.selectedBy()).isEqualTo("credentialsRef");
        assertThat(report.toMap())
                .containsEntry("selectedCredentialsId", "tenant-a-readiness")
                .containsEntry("selectedBy", "credentialsRef")
                .containsEntry("available", true);
    }

    @Test
    void credentialRegistryFallsBackToProviderAndDefaultCredentials() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.builder()
                        .register("default", WayangReadinessProfileS3Credentials.of("default-access", "default-secret"))
                        .register("minio", WayangReadinessProfileS3Credentials.of("minio-access", "minio-secret"))
                        .build();

        S3StorageConfig providerStorage = WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                WayangObjectStorageConfig.fromMap(java.util.Map.of(
                        "provider", "minio",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "objectKey", "profiles/default.properties")),
                registry);
        S3StorageConfig defaultStorage = WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                WayangObjectStorageConfig.s3(
                        "",
                        "wayang",
                        "",
                        "profiles/default.properties"),
                registry);

        assertThat(providerStorage.accessKeyId()).isEqualTo("minio-access");
        assertThat(defaultStorage.accessKeyId()).isEqualTo("default-access");
        assertThat(registry.resolveReport(WayangObjectStorageConfig.fromMap(java.util.Map.of(
                "provider", "minio",
                "bucket", "wayang",
                "objectKey", "profiles/default.properties"))).selectedBy())
                .isEqualTo("provider");
        assertThat(registry.resolveReport(WayangObjectStorageConfig.s3(
                "",
                "wayang",
                "",
                "profiles/default.properties")).selectedBy())
                .isEqualTo("default");
    }

    @Test
    void factoryCanCreateReaderFromCredentialResolver() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.ofDefault(
                        WayangReadinessProfileS3Credentials.of("access", "secret"));

        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileS3ObjectStorageReaderFactory.reader(
                        WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/default.properties"),
                        registry);

        assertThat(reader).isNotNull();
    }

    @Test
    void credentialRegistryReportsMissingCredentials() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.empty();
        WayangObjectStorageConfig config = new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "tenant-a-readiness");

        assertThat(registry.resolveReport(config).available()).isFalse();
        assertThat(registry.resolveReport(config).selectedBy()).isEqualTo("none");
        assertThatThrownBy(() -> WayangReadinessProfileS3ObjectStorageReaderFactory.reader(config, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentialsRef 'tenant-a-readiness'");
    }

    @Test
    void factoryMissingCredentialsMessageRedactsInlineCredentialsRef() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.empty();
        WayangObjectStorageConfig config = new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "accessKeyId=inline-access secretAccessKey=inline-secret");

        assertThatThrownBy(() -> WayangReadinessProfileS3ObjectStorageReaderFactory.reader(config, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("<redacted>")
                .hasMessageNotContaining("inline-access")
                .hasMessageNotContaining("inline-secret");
    }

    @Test
    void rejectsMissingCredentials() {
        assertThatThrownBy(() -> WayangReadinessProfileS3ObjectStorageReaderFactory.toS3StorageConfig(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"),
                (WayangReadinessProfileS3Credentials) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials are required");
        assertThatThrownBy(() -> WayangReadinessProfileS3Credentials.of("", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessKeyId");
    }
}
