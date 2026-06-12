package tech.kayys.wayang.gollek.sdk.storage.s3;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessProfileS3ObjectStoragePreflightTest {

    @Test
    void preflightCombinesCredentialDiagnosticsAndObjectValidation() {
        WayangReadinessProfileS3CredentialSource source =
                WayangReadinessProfileS3CredentialSource.fromMap(
                        "rustfs",
                        Map.of(
                                "accessKeyId", "access",
                                "secretAccessKey", "super-secret"));
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.fromSources(List.of(source));
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument());

        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(registry, (config, credentials) -> storage)
                        .check(WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/default.properties"));

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.credentialsResolution().selectedCredentialsId()).isEqualTo("rustfs");
        assertThat(report.serviceCreated()).isTrue();
        assertThat(report.objectStoragePreflight()).isNotNull();
        assertThat(report.objectStoragePreflight().profileIds())
                .containsExactly("object-default", "object-production");
        assertThat(report.credentialSourceDiagnostics()).hasSize(1);
        assertThat(report.toMap().toString()).doesNotContain("super-secret");
    }

    @Test
    void preflightDoesNotCreateServiceWhenCredentialsAreMissing() {
        AtomicBoolean serviceCreated = new AtomicBoolean(false);
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.empty();

        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(registry, (config, credentials) -> {
                            serviceCreated.set(true);
                            return new InMemoryObjectStorageService();
                        })
                        .check(WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.serviceCreated()).isFalse();
        assertThat(serviceCreated).isFalse();
        assertThat(report.objectStoragePreflight()).isNull();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("No S3 readiness profile credentials"));
        assertThat(report.toMap().get("objectStoragePreflight")).isEqualTo(Map.of());
    }

    @Test
    void preflightReportsServiceCreationFailure() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.ofDefault(
                        WayangReadinessProfileS3Credentials.of("access", "secret"));

        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(registry, (config, credentials) -> {
                            throw new IllegalStateException("boom");
                        })
                        .check(WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.credentialsResolution().available()).isTrue();
        assertThat(report.serviceCreated()).isFalse();
        assertThat(report.message()).contains("could not create");
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("boom"));
    }

    @Test
    void preflightIncludesObjectPreflightIssues() {
        WayangReadinessProfileS3CredentialsRegistry registry =
                WayangReadinessProfileS3CredentialsRegistry.ofDefault(
                        WayangReadinessProfileS3Credentials.of("access", "secret"));

        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(registry, (config, credentials) -> new InMemoryObjectStorageService())
                        .check(WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/missing.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.serviceCreated()).isTrue();
        assertThat(report.objectStoragePreflight()).isNotNull();
        assertThat(report.objectStoragePreflight().ready()).isFalse();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("profiles/missing.properties"));
    }

    @Test
    void preflightReportsExternalCredentialResolverFailure() {
        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(config -> {
                            throw new IllegalStateException("credentials boom");
                        }, (config, credentials) -> new InMemoryObjectStorageService())
                        .check(WayangObjectStorageConfig.rustfs(
                                "http://localhost:9000",
                                "wayang",
                                "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.credentialsResolution().available()).isFalse();
        assertThat(report.serviceCreated()).isFalse();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("credentials boom"));
    }

    @Test
    void preflightRedactsCredentialValuesFromExternalResolverFailureDiagnostics() {
        WayangReadinessProfileS3ObjectStoragePreflightReport report =
                WayangReadinessProfileS3ObjectStoragePreflight
                        .usingServiceFactory(config -> {
                            throw new IllegalStateException(
                                    "secretAccessKey=inline-secret accessKeyId=inline-access");
                        }, (config, credentials) -> new InMemoryObjectStorageService())
                        .check(new WayangObjectStorageConfig(
                                "s3",
                                "https://s3.example.test",
                                "wayang",
                                "ap-southeast-1",
                                "profiles/default.properties",
                                false,
                                "accessKeyId=ref-access secretAccessKey=ref-secret"));

        assertThat(report.toMap().toString())
                .contains("<redacted>")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-access")
                .doesNotContain("ref-secret")
                .doesNotContain("ref-access");
    }

    private static String readinessProfileDocument() {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=object-default,object-production
                defaultProfileId=object-default
                productionProfileId=object-production
                profile.object-default.description=Object default profile.
                profile.object-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.object-production.description=Object production profile.
                profile.object-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """;
    }

    private static final class InMemoryObjectStorageService implements ObjectStorageService {

        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        InMemoryObjectStorageService withObject(String key, String value) {
            objects.put(key, value.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        @Override
        public Uni<Optional<byte[]>> getObject(String key) {
            return Uni.createFrom().item(Optional.ofNullable(objects.get(key)));
        }

        @Override
        public Uni<List<String>> listObjects(String prefix) {
            return Uni.createFrom().item(objects.keySet().stream()
                    .filter(key -> key.startsWith(prefix == null ? "" : prefix))
                    .toList());
        }

        @Override
        public Uni<Void> putObject(String key, byte[] data) {
            objects.put(key, data == null ? new byte[0] : data);
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Boolean> deleteObject(String key) {
            return Uni.createFrom().item(objects.remove(key) != null);
        }
    }
}
