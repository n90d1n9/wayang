package tech.kayys.wayang.gollek.sdk.storage;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.LocalWayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangReadinessProfileObjectStorageServiceReaderTest {

    @Test
    void readsConfiguredReadinessProfileObjectKey() throws Exception {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument());
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.of(storage);

        String document = reader.read(WayangObjectStorageConfig.rustfs(
                "http://localhost:9000",
                "wayang",
                "profiles/default.properties"));

        assertThat(document).contains("profileIds=object-default,object-production");
    }

    @Test
    void canOverrideConfiguredObjectKey() throws Exception {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("operators/readiness.properties", readinessProfileDocument());
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.of(storage)
                        .withObjectKey("operators/readiness.properties")
                        .withTimeout(Duration.ofSeconds(2))
                        .withCharset(StandardCharsets.UTF_8);

        String document = reader.read(WayangObjectStorageConfig.rustfs(
                "http://localhost:9000",
                "wayang",
                "profiles/default.properties"));

        assertThat(document).contains("productionProfileId=object-production");
    }

    @Test
    void reportsMissingObjectAsFileNotFound() {
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.of(new InMemoryObjectStorageService());

        assertThatThrownBy(() -> reader.read(WayangObjectStorageConfig.rustfs(
                "http://localhost:9000",
                "wayang",
                "profiles/missing.properties")))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("profiles/missing.properties");
    }

    @Test
    void requiresObjectKey() {
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.of(new InMemoryObjectStorageService());

        assertThatThrownBy(() -> reader.read(WayangObjectStorageConfig.rustfs(
                "http://localhost:9000",
                "wayang",
                "")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("object key is not configured");
    }

    @Test
    void registryResolverPrefersCredentialsRefOverProvider() throws Exception {
        InMemoryObjectStorageService providerStorage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument("provider-default"));
        InMemoryObjectStorageService credentialStorage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument("credential-default"));
        WayangReadinessProfileObjectStorageServiceRegistry registry =
                WayangReadinessProfileObjectStorageServiceRegistry.builder()
                        .register("rustfs", providerStorage)
                        .register("tenant-a-readiness", credentialStorage)
                        .build();
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.resolving(registry);

        WayangReadinessProfileObjectStorageServiceResolution report = registry.resolveReport(
                new WayangObjectStorageConfig(
                        "rustfs",
                        "http://localhost:9000",
                        "wayang",
                        "",
                        "profiles/default.properties",
                        true,
                        "tenant-a-readiness"));
        String document = reader.read(new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "tenant-a-readiness"));

        assertThat(document).contains("defaultProfileId=credential-default");
        assertThat(report.available()).isTrue();
        assertThat(report.selectedServiceId()).isEqualTo("tenant-a-readiness");
        assertThat(report.selectedBy()).isEqualTo("credentialsRef");
        assertThat(report.toMap())
                .containsEntry("selectedServiceId", "tenant-a-readiness")
                .containsEntry("selectedBy", "credentialsRef")
                .containsEntry("available", true);
        assertThat(registry.serviceIds()).containsExactly("rustfs", "tenant-a-readiness");
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    void registryResolverFallsBackToProviderAndDefaultServices() throws Exception {
        InMemoryObjectStorageService providerStorage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument("provider-default"));
        InMemoryObjectStorageService defaultStorage = new InMemoryObjectStorageService()
                .withObject("profiles/fallback.properties", readinessProfileDocument("default-profile"));
        WayangReadinessProfileObjectStorageServiceRegistry registry =
                WayangReadinessProfileObjectStorageServiceRegistry.builder()
                        .register("default", defaultStorage)
                        .register("minio", providerStorage)
                        .build();
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.resolving(registry);

        String providerDocument = reader.read(WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "minio",
                "bucket", "wayang",
                "endpoint", "http://localhost:9000",
                "objectKey", "profiles/default.properties")));
        String defaultDocument = reader.read(WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "s3",
                "bucket", "wayang",
                "objectKey", "profiles/fallback.properties")));

        assertThat(registry.resolveReport(WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "minio",
                "bucket", "wayang",
                "objectKey", "profiles/default.properties"))).selectedBy())
                .isEqualTo("provider");
        assertThat(registry.resolveReport(WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "s3",
                "bucket", "wayang",
                "objectKey", "profiles/fallback.properties"))).selectedBy())
                .isEqualTo("default");
        assertThat(providerDocument).contains("defaultProfileId=provider-default");
        assertThat(defaultDocument).contains("defaultProfileId=default-profile");
    }

    @Test
    void readerReportsMissingResolvedService() {
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.resolving(
                        WayangReadinessProfileObjectStorageServiceRegistry.empty());
        WayangReadinessProfileObjectStorageServiceResolution report =
                WayangReadinessProfileObjectStorageServiceRegistry.empty().resolveReport(
                        new WayangObjectStorageConfig(
                                "rustfs",
                                "http://localhost:9000",
                                "wayang",
                                "",
                                "profiles/default.properties",
                                true,
                                "tenant-a-readiness"));

        assertThatThrownBy(() -> reader.read(new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                "tenant-a-readiness")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("credentialsRef 'tenant-a-readiness'");
        assertThat(report.available()).isFalse();
        assertThat(report.selectedBy()).isEqualTo("none");
        assertThat(report.serviceIds()).isEmpty();
    }

    @Test
    void serviceResolutionMapRedactsInlineCredentialsRefButKeepsRawAccessors() {
        String credentialsRef = "accessKeyId=inline-access secretAccessKey=inline-secret";
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument());
        WayangReadinessProfileObjectStorageServiceRegistry registry =
                WayangReadinessProfileObjectStorageServiceRegistry.builder()
                        .register(credentialsRef, storage)
                        .build();
        WayangObjectStorageConfig config = new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                credentialsRef);

        WayangReadinessProfileObjectStorageServiceResolution report = registry.resolveReport(config);
        String output = report.toMap().toString();

        assertThat(report.available()).isTrue();
        assertThat(report.selectedServiceId()).contains("inline-secret");
        assertThat(registry.resolve(config)).isPresent();
        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret");
    }

    @Test
    void readerMissingServiceMessageRedactsInlineCredentialsRef() {
        String credentialsRef = "accessKeyId=inline-access secretAccessKey=inline-secret";
        WayangReadinessProfileObjectStorageServiceReader reader =
                WayangReadinessProfileObjectStorageServiceReader.resolving(
                        WayangReadinessProfileObjectStorageServiceRegistry.empty());

        assertThatThrownBy(() -> reader.read(new WayangObjectStorageConfig(
                "rustfs",
                "http://localhost:9000",
                "wayang",
                "",
                "profiles/default.properties",
                true,
                credentialsRef)))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("<redacted>")
                .hasMessageNotContaining("inline-access")
                .hasMessageNotContaining("inline-secret");
    }

    @Test
    void localSdkCanUseStorageServiceReaderForObjectStorageProfiles() {
        InMemoryObjectStorageService storage = new InMemoryObjectStorageService()
                .withObject("profiles/default.properties", readinessProfileDocument());
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "validationPolicy", "strict"))),
                WayangReadinessProfileObjectStorageServiceReader.of(storage));

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("object-default", "object-production");
        assertThat(sdk.platformReadiness().ready()).isTrue();
    }

    private static String readinessProfileDocument() {
        return readinessProfileDocument("object-default");
    }

    private static String readinessProfileDocument(String defaultProfileId) {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=object-default,object-production
                defaultProfileId=%s
                productionProfileId=object-production
                profile.object-default.description=Object default profile.
                profile.object-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.credential-default.description=Credential default profile.
                profile.credential-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.provider-default.description=Provider default profile.
                profile.provider-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.default-profile.description=Fallback default profile.
                profile.default-profile.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.object-production.description=Object production profile.
                profile.object-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """.formatted(defaultProfileId);
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
