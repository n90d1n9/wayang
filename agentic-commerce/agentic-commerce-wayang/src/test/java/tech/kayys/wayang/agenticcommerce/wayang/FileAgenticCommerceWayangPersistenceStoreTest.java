package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileAgenticCommerceWayangPersistenceStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingFilesReturnEmptyAndExposeStoreStatus() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));

        assertThat(store.loadRuntimeConfig()).isEmpty();
        assertThat(store.loadBootstrapConfig()).isEmpty();
        assertThat(store.loadBootstrapReport()).isEmpty();
        assertThat(store.loadManifest()).isEmpty();
        assertThat(store.toMap())
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("runtimeConfigAvailable", false)
                .containsEntry("bootstrapConfigAvailable", false)
                .containsEntry("bootstrapReportAvailable", false)
                .containsEntry("manifestAvailable", false);
    }

    @Test
    void runtimeAndBootstrapConfigRoundTripThroughJsonFiles() throws Exception {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                        "mode",
                        "seller-http",
                        "metadata",
                        Map.of("tenant", "acme"))))
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-secret")
                        .withBaseUrl("https://seller.example/")
                        .withHeaders(Map.of("X-Seller", "demo"))
                        .withAttributes(Map.of("tenant", "acme")))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build())
                .build();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .requireSkillRegistration(false)
                .build();

        store.saveRuntimeConfig(runtimeConfig);
        store.saveBootstrapConfig(bootstrapConfig);

        AgenticCommerceWayangRuntimeConfig loadedRuntimeConfig = store.loadRuntimeConfig().orElseThrow();
        AgenticCommerceWayangBootstrapConfig loadedBootstrapConfig = store.loadBootstrapConfig().orElseThrow();
        Map<String, Object> rawRuntimeConfig = AgenticCommerceJson.readObject(
                Files.readString(store.runtimeConfigPath(), StandardCharsets.UTF_8));
        Map<String, Object> storedConnectorFactoryConfig = map(rawRuntimeConfig.get("connectorFactoryConfig"));
        Map<String, Object> storedConnectorConfig = map(rawRuntimeConfig.get("connectorConfig"));
        Map<String, Object> storedConnectorPolicy = map(rawRuntimeConfig.get("connectorPolicy"));
        Map<String, Object> diagnosticConnectorFactoryConfig = map(runtimeConfig.toMap().get("connectorFactoryConfig"));
        Map<String, Object> diagnosticConnectorConfig = map(runtimeConfig.toMap().get("connectorConfig"));
        Map<String, Object> diagnosticConnectorPolicy = map(runtimeConfig.toMap().get("connectorPolicy"));

        assertThat(loadedRuntimeConfig.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(loadedRuntimeConfig.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(loadedRuntimeConfig.connectorConfig().bearerToken()).isEqualTo("seller-secret");
        assertThat(loadedRuntimeConfig.connectorConfig().headers()).containsEntry("X-Seller", "demo");
        assertThat(loadedRuntimeConfig.connectorPolicy().allowedBaseHosts()).containsExactly("seller.example");
        assertThat(loadedRuntimeConfig.connectorPolicy().requireHttps()).isTrue();
        assertThat(loadedRuntimeConfig.httpConfig().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(loadedBootstrapConfig.skillIds()).containsExactly(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        assertThat(loadedBootstrapConfig.includeRuntimeSkills()).isFalse();
        assertThat(loadedBootstrapConfig.requireSkillRegistration()).isFalse();
        assertThat(storedConnectorFactoryConfig)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP)
                .doesNotContainKey("httpConnector");
        assertThat(storedConnectorConfig).containsEntry("bearerToken", "seller-secret");
        assertThat(storedConnectorPolicy)
                .containsEntry("allowedConnectorKinds", List.of(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP))
                .containsEntry("allowedBaseHosts", List.of("seller.example"))
                .containsEntry("requireHttps", true)
                .doesNotContainKey("baseHostRestricted");
        assertThat(diagnosticConnectorFactoryConfig).containsEntry("httpConnector", true);
        assertThat(diagnosticConnectorConfig)
                .containsEntry("bearerTokenConfigured", true)
                .doesNotContainKey("bearerToken");
        assertThat(diagnosticConnectorPolicy).containsEntry("baseHostRestricted", true);
    }

    @Test
    void persistsBootstrapReportAndManifestSnapshots() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .build();
        AgenticCommerceWayangBootstrapReport report =
                runtime.bootstrap(new AgenticCommerceTestSkillRegistry(), bootstrapConfig);
        AgenticCommerceWayangManifest manifest = runtime.manifest(bootstrapConfig);

        store.saveBootstrapReport(report);
        store.saveManifest(manifest);

        Map<String, Object> loadedReport = store.loadBootstrapReport().orElseThrow();
        Map<String, Object> loadedManifest = store.loadManifest().orElseThrow();
        Map<String, Object> reportRegistration = map(loadedReport.get("skillRegistration"));
        Map<String, Object> manifestBootstrapConfig = map(loadedManifest.get("bootstrapConfig"));

        assertThat(loadedReport).containsEntry("ready", true);
        assertThat(number(reportRegistration.get("definitionCount"))).isEqualTo(1);
        assertThat(number(reportRegistration.get("runtimeSkillCount"))).isZero();
        assertThat(number(loadedManifest.get("skillCount"))).isEqualTo(5);
        assertThat(manifestBootstrapConfig)
                .containsEntry("skillIds", List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .containsEntry("includeRuntimeSkills", false);
        assertThat(store.toMap())
                .containsEntry("bootstrapReportAvailable", true)
                .containsEntry("manifestAvailable", true);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static int number(Object value) {
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).intValue();
    }
}
