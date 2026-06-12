package tech.kayys.wayang.gollek.sdk.storage;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.LocalWayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProvider;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviders;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryPreflightReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryResolution;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessProfileObjectStorageExternalReaderProviderTest {

    @Test
    void registersExternalReaderProviderThroughServiceLoader() {
        assertThat(ServiceLoader.load(WayangPlatformReadinessProfileExternalReaderProvider.class))
                .anySatisfy(provider -> assertThat(provider)
                        .isInstanceOf(WayangReadinessProfileObjectStorageExternalReaderProvider.class));
    }

    @Test
    void discoversObjectStorageReaderFromServiceProvider() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY,
                        "validationPolicy", "strict"))));

        WayangPlatformReadinessProfileRegistryResolution resolution =
                sdk.platformReadinessProfileRegistryResolution();

        assertThat(sdk.platformReadinessProfiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("storage-discovered-default", "storage-discovered-production");
        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.activeSourceId()).isEqualTo("rustfs");
        assertThat(resolution.activeSourceType()).isEqualTo("object_storage");
        assertThat(resolution.activeSourceLocation())
                .isEqualTo("rustfs://wayang/"
                        + TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY);
    }

    @Test
    void buildsEmptyRegistryWhenNoObjectStorageProviderMatchesConfig() {
        WayangReadinessProfileObjectStorageServiceRegistry registry =
                WayangReadinessProfileObjectStorageServiceProviders.registry(WayangGollekSdkConfig.local()
                        .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                                "mode", "s3",
                                "endpoint", "https://s3.example.invalid",
                                "bucket", "wayang",
                                "keyPrefix", "profiles/default.properties"))));

        assertThat(registry.size()).isZero();
    }

    @Test
    void collectsObjectStorageProviderDiagnostics() {
        List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> diagnostics =
                WayangReadinessProfileObjectStorageServiceProviders.diagnostics(WayangGollekSdkConfig.local()
                        .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                                "mode", "rustfs",
                                "endpoint", "http://localhost:9000",
                                "bucket", "wayang",
                                "keyPrefix", TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY))));

        assertThat(diagnostics)
                .anySatisfy(diagnostic -> {
                    assertThat(diagnostic.providerId())
                            .isEqualTo("test-object-storage-readiness-provider");
                    assertThat(diagnostic.available()).isTrue();
                    assertThat(diagnostic.serviceIds()).containsExactly("rustfs");
                    assertThat(diagnostic.toMap())
                            .containsEntry("serviceCount", 1)
                            .containsEntry("available", true);
                });
    }

    @Test
    void buildsObjectStorageServiceDiscoveryReport() {
        WayangReadinessProfileObjectStorageServiceDiscoveryReport report =
                WayangReadinessProfileObjectStorageServiceProviders.discoveryReport(WayangGollekSdkConfig.local()
                        .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                                "mode", "rustfs",
                                "endpoint", "http://localhost:9000",
                                "bucket", "wayang",
                                "keyPrefix", TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY))));

        assertThat(report.available()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.providerCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.availableProviderCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.serviceIds()).contains("rustfs");
        assertThat(report.toMap())
                .containsEntry("available", true)
                .containsEntry("serviceCount", report.serviceCount());
    }

    @Test
    void providerDiagnosticsAndDiscoveryMapsRedactNestedCredentialDetails() {
        String serviceId = "accessKeyId=inline-access secretAccessKey=inline-secret";
        WayangReadinessProfileObjectStorageServiceProviderDiagnostics diagnostic =
                new WayangReadinessProfileObjectStorageServiceProviderDiagnostics(
                        "test-provider",
                        10,
                        true,
                        List.of(serviceId),
                        Map.of(
                                "serviceIds", List.of(serviceId),
                                "nested", Map.of("message", "token=inline-token")),
                        "password=inline-password");
        WayangReadinessProfileObjectStorageServiceDiscoveryReport report =
                WayangReadinessProfileObjectStorageServiceDiscoveryReport.of(List.of(diagnostic));

        assertThat(diagnostic.serviceIds()).contains(serviceId.toLowerCase(java.util.Locale.ROOT));
        assertThat(report.serviceIds()).contains(serviceId.toLowerCase(java.util.Locale.ROOT));
        assertThat(diagnostic.toMap().toString())
                .contains("<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token")
                .doesNotContain("inline-password");
        assertThat(report.toMap().toString())
                .contains("<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token")
                .doesNotContain("inline-password");
    }

    @Test
    void publishesObjectStorageReportThroughNeutralExternalReaderDiagnostics() {
        WayangGollekSdkConfig config = WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY)));
        WayangPlatformReadinessProfileExternalReaderProvider provider =
                new WayangReadinessProfileObjectStorageExternalReaderProvider();

        WayangPlatformReadinessProfileExternalReaderProviderDiagnostics diagnostics =
                provider.diagnostics(config);
        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report =
                WayangPlatformReadinessProfileExternalReaderProviders.discoveryReport(config);

        assertThat(diagnostics.available()).isTrue();
        assertThat(diagnostics.readerTypes()).containsExactly("object_storage");
        assertThat(diagnostics.details()).containsKey("objectStorage");
        assertThat(report.ready()).isTrue();
        assertThat(report.requiredReaderTypes()).containsExactly("object_storage");
        assertThat(report.availableReaderTypes()).contains("object_storage");
        assertThat(report.providers())
                .anySatisfy(discovered -> assertThat(discovered.providerId())
                        .isEqualTo("wayang-readiness-profile-object-storage"));
    }

    @Test
    void registryPreflightPassesWhenObjectStorageReaderProviderIsAvailable() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "rustfs",
                        "endpoint", "http://localhost:9000",
                        "bucket", "wayang",
                        "keyPrefix", TestReadinessProfileObjectStorageServiceProvider.OBJECT_KEY,
                        "validationPolicy", "strict"))));

        WayangPlatformReadinessProfileRegistryPreflightReport report =
                sdk.platformReadinessProfileRegistryPreflight();

        assertThat(report.ready()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.providerDiscovery().requiredReaderTypes()).containsExactly("object_storage");
        assertThat(report.providerDiscovery().availableReaderTypes()).contains("object_storage");
        assertThat(report.registryReady()).isTrue();
        assertThat(report.fallbackUsed()).isFalse();
    }
}
