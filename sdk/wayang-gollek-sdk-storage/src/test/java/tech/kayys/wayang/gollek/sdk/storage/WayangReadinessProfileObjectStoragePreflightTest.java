package tech.kayys.wayang.gollek.sdk.storage;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessProfileObjectStoragePreflightTest {

    @Test
    void preflightReadsObjectAndValidatesProfiles() {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument());
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(storage))
                        .withTimeout(Duration.ofSeconds(2));

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"));

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.objectReadable()).isTrue();
        assertThat(report.documentBytes()).isGreaterThan(0);
        assertThat(report.documentCharacters()).isGreaterThan(0);
        assertThat(report.profileIds()).containsExactly("object-default", "object-production");
        assertThat(report.registryResolution()).isNotNull();
        assertThat(report.registryResolution().valid()).isTrue();
        assertThat(report.toMap())
                .containsEntry("ready", true)
                .containsEntry("objectReadable", true)
                .containsEntry("profileCount", 2);
    }

    @Test
    void preflightReportsMissingServiceWithoutReadingObject() {
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.empty());

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.objectReadable()).isFalse();
        assertThat(report.serviceResolution().available()).isFalse();
        assertThat(report.registryResolution()).isNull();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("No readiness profile object-storage service"));
        assertThat(report.toMap().get("registryResolution")).isEqualTo(Map.of());
    }

    @Test
    void preflightReportsMissingObject() {
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(
                                new InMemoryObjectStorageService()));

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/missing.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.serviceResolution().available()).isTrue();
        assertThat(report.objectReadable()).isFalse();
        assertThat(report.documentBytes()).isZero();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue).contains("profiles/missing.properties"));
    }

    @Test
    void preflightReportsInvalidProfileDocumentAfterRead() {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", "schema=wrong\nprofileIds=broken\n");
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(storage));

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.objectReadable()).isTrue();
        assertThat(report.registryResolution()).isNotNull();
        assertThat(report.registryResolution().valid()).isFalse();
        assertThat(report.message()).contains("validation failed");
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .contains("Unsupported readiness profile document schema"));
    }

    @Test
    void preflightReportsEmptyReadableObject() {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", "");
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(storage));

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs(
                        "http://localhost:9000",
                        "wayang",
                        "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.objectReadable()).isTrue();
        assertThat(report.registryResolution()).isNull();
        assertThat(report.message()).doesNotContain("passed");
        assertThat(report.issues()).contains("Readiness profile object document is empty.");
    }

    @Test
    void preflightReportsInvalidObjectStorageConfigBeforeRead() {
        WayangReadinessProfileObjectStoragePreflight preflight =
                WayangReadinessProfileObjectStoragePreflight.registry(
                        WayangReadinessProfileObjectStorageServiceRegistry.ofDefault(
                                new InMemoryObjectStorageService()));

        WayangReadinessProfileObjectStoragePreflightReport report = preflight.check(
                WayangObjectStorageConfig.rustfs("", "wayang", "profiles/default.properties"));

        assertThat(report.ready()).isFalse();
        assertThat(report.configDiagnostics().valid()).isFalse();
        assertThat(report.objectReadable()).isFalse();
        assertThat(report.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .contains("readiness_profile_object_endpoint_required"));
    }

    @Test
    void preflightReportRedactsInlineCredentialValuesFromAggregatedFailures() {
        WayangObjectStorageConfig objectStorage = new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "accessKeyId=ref-access secretAccessKey=ref-secret");
        WayangReadinessProfileObjectStorageServiceResolution serviceResolution =
                new WayangReadinessProfileObjectStorageServiceResolution(
                        objectStorage.credentialsRef(),
                        objectStorage.provider(),
                        objectStorage.credentialsRef(),
                        "credentialsRef",
                        true,
                        List.of(objectStorage.credentialsRef()),
                        "selected token=inline-token");

        WayangReadinessProfileObjectStoragePreflightReport report =
                WayangReadinessProfileObjectStoragePreflightReport.from(
                        objectStorage,
                        objectStorage.keyPrefix(),
                        WayangPlatformReadinessProfileRegistryConfig
                                .objectStorage(objectStorage, false)
                                .diagnostics(),
                        serviceResolution,
                        null,
                        "",
                        false,
                        StandardCharsets.UTF_8,
                        new IllegalStateException(
                                "secretAccessKey=inline-secret accessKeyId=inline-access"),
                        List.of("apiKey=inline-api-key"));
        String output = report.toMap().toString();

        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("ref-access")
                .doesNotContain("ref-secret")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-access")
                .doesNotContain("inline-api-key")
                .doesNotContain("inline-token");
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
