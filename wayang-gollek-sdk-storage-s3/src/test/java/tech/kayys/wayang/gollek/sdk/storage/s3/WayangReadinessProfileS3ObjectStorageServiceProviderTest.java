package tech.kayys.wayang.gollek.sdk.storage.s3;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceProvider;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceProviderDiagnostics;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceRegistration;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStorageServiceProviders;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessProfileS3ObjectStorageServiceProviderTest {

    private static final String PROFILE_KEY = "profiles/default.properties";

    @Test
    void registersObjectStorageServiceProviderThroughServiceLoader() {
        assertThat(ServiceLoader.load(WayangReadinessProfileObjectStorageServiceProvider.class))
                .anySatisfy(provider -> assertThat(provider)
                        .isInstanceOf(WayangReadinessProfileS3ObjectStorageServiceProvider.class));
    }

    @Test
    void createsRustfsServiceRegistrationFromEnvironmentCredentials() {
        AtomicReference<WayangReadinessProfileS3Credentials> capturedCredentials =
                new AtomicReference<>();
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.MINIO_ROOT_USER_ENV,
                                "minio-user",
                                WayangReadinessProfileS3CredentialSource.MINIO_ROOT_PASSWORD_ENV,
                                "minio-secret"),
                        Map.of(),
                        (objectStorage, credentials) -> {
                            capturedCredentials.set(credentials);
                            assertThat(objectStorage.provider()).isEqualTo("rustfs");
                            assertThat(objectStorage.bucket()).isEqualTo("wayang");
                            return new NoopObjectStorageService();
                        });

        List<WayangReadinessProfileObjectStorageServiceRegistration> services =
                provider.services(config("rustfs", "", PROFILE_KEY));
        WayangReadinessProfileS3ObjectStorageServiceProviderReport report =
                provider.report(config("rustfs", "", PROFILE_KEY));

        assertThat(services).singleElement()
                .satisfies(registration -> {
                    assertThat(registration.serviceId()).isEqualTo("rustfs");
                    assertThat(registration.service()).isInstanceOf(NoopObjectStorageService.class);
                });
        assertThat(capturedCredentials.get().accessKeyId()).isEqualTo("minio-user");
        assertThat(capturedCredentials.get().secretAccessKey()).isEqualTo("minio-secret");
        assertThat(report.available()).isTrue();
        assertThat(report.serviceCreated()).isTrue();
        assertThat(report.serviceId()).isEqualTo("rustfs");
        assertThat(report.issues()).isEmpty();
        assertThat(report.toMap().toString()).doesNotContain("minio-secret");
    }

    @Test
    void prefersCredentialsRefServiceIdWhenConfigured() {
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_ENV,
                                "tenant-key",
                                WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_ENV,
                                "tenant-secret"),
                        Map.of(),
                        (objectStorage, credentials) -> new NoopObjectStorageService());

        List<WayangReadinessProfileObjectStorageServiceRegistration> services =
                provider.services(config("s3", "tenant-a-readiness", PROFILE_KEY));

        assertThat(services).singleElement()
                .satisfies(registration -> assertThat(registration.serviceId())
                        .isEqualTo("tenant-a-readiness"));
    }

    @Test
    void usesSystemPropertiesWhenEnvironmentCredentialsAreMissing() {
        AtomicReference<WayangReadinessProfileS3Credentials> capturedCredentials =
                new AtomicReference<>();
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(),
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_PROPERTY,
                                "property-key",
                                WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_PROPERTY,
                                "property-secret"),
                        (objectStorage, credentials) -> {
                            capturedCredentials.set(credentials);
                            return new NoopObjectStorageService();
                        });

        List<WayangReadinessProfileObjectStorageServiceRegistration> services =
                provider.services(config("minio", "", PROFILE_KEY));

        assertThat(services).hasSize(1);
        assertThat(capturedCredentials.get().accessKeyId()).isEqualTo("property-key");
        assertThat(capturedCredentials.get().secretAccessKey()).isEqualTo("property-secret");
    }

    @Test
    void skipsUnsupportedProvidersAndIncompleteObjectConfigs() {
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_ENV,
                                "access",
                                WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_ENV,
                                "secret"),
                        Map.of(),
                        (objectStorage, credentials) -> new NoopObjectStorageService());

        assertThat(provider.services(config("gcs", "", PROFILE_KEY))).isEmpty();
        assertThat(provider.services(config("rustfs", "", ""))).isEmpty();
        assertThat(provider.report(config("gcs", "", PROFILE_KEY)).issues())
                .anySatisfy(issue -> assertThat(issue).contains("not S3-compatible"));
        assertThat(provider.report(config("rustfs", "", "")).issues())
                .anySatisfy(issue -> assertThat(issue).contains("bucket and object key"));
    }

    @Test
    void reportsMissingCredentialsWithoutCreatingService() {
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(),
                        Map.of(),
                        (objectStorage, credentials) -> new NoopObjectStorageService());

        WayangReadinessProfileS3ObjectStorageServiceProviderReport report =
                provider.report(config("rustfs", "", PROFILE_KEY));

        assertThat(provider.services(config("rustfs", "", PROFILE_KEY))).isEmpty();
        assertThat(report.available()).isFalse();
        assertThat(report.credentialsResolution().available()).isFalse();
        assertThat(report.serviceCreated()).isFalse();
        assertThat(report.message()).contains("could not resolve credentials");
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("No S3 readiness profile credentials"));
    }

    @Test
    void redactsInlineCredentialsRefInS3ProviderReportMaps() {
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(),
                        Map.of(),
                        (objectStorage, credentials) -> new NoopObjectStorageService());
        WayangGollekSdkConfig config = config(
                "s3",
                "accessKeyId=inline-access secretAccessKey=inline-secret",
                PROFILE_KEY);

        WayangReadinessProfileS3ObjectStorageServiceProviderReport report = provider.report(config);

        assertThat(report.serviceId())
                .contains("inline-access")
                .contains("inline-secret");
        assertThat(report.toMap().toString())
                .contains("<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret");
    }

    @Test
    void reportsServiceCreationFailure() {
        WayangReadinessProfileS3ObjectStorageServiceProvider provider =
                new WayangReadinessProfileS3ObjectStorageServiceProvider(
                        Map.of(
                                WayangReadinessProfileS3CredentialSource.ACCESS_KEY_ID_ENV,
                                "access",
                                WayangReadinessProfileS3CredentialSource.SECRET_ACCESS_KEY_ENV,
                                "secret"),
                        Map.of(),
                        (objectStorage, credentials) -> {
                            throw new IllegalStateException("cannot initialize client");
                        });

        WayangReadinessProfileS3ObjectStorageServiceProviderReport report =
                provider.report(config("rustfs", "", PROFILE_KEY));

        assertThat(provider.services(config("rustfs", "", PROFILE_KEY))).isEmpty();
        assertThat(report.available()).isFalse();
        assertThat(report.credentialsResolution().available()).isTrue();
        assertThat(report.serviceCreated()).isFalse();
        assertThat(report.message()).contains("could not be created");
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("cannot initialize client"));
    }

    @Test
    void publishesS3ProviderReportThroughNeutralDiagnostics() {
        List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> diagnostics =
                WayangReadinessProfileObjectStorageServiceProviders.diagnostics(config("gcs", "", PROFILE_KEY));

        assertThat(diagnostics)
                .anySatisfy(diagnostic -> {
                    assertThat(diagnostic.providerId())
                            .isEqualTo("wayang-readiness-profile-s3-object-storage");
                    assertThat(diagnostic.available()).isFalse();
                    assertThat(diagnostic.details())
                            .containsEntry("supportedProvider", false)
                            .containsEntry("available", false);
                    assertThat(diagnostic.message()).contains("unsupported provider");
                    assertThat(diagnostic.serviceIds()).isEmpty();
                    assertThat(diagnostic.toMap().toString()).doesNotContain("tenant-secret");
                });
    }

    private static WayangGollekSdkConfig config(
            String provider,
            String credentialsRef,
            String keyPrefix) {
        return WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.objectStorage(
                        new WayangObjectStorageConfig(
                                provider,
                                "http://localhost:9000",
                                "wayang",
                                "",
                                keyPrefix,
                                true,
                                credentialsRef),
                        false));
    }

    private static final class NoopObjectStorageService implements ObjectStorageService {

        @Override
        public Uni<Optional<byte[]>> getObject(String key) {
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<List<String>> listObjects(String prefix) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Void> putObject(String key, byte[] data) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Boolean> deleteObject(String key) {
            return Uni.createFrom().item(false);
        }
    }

}
