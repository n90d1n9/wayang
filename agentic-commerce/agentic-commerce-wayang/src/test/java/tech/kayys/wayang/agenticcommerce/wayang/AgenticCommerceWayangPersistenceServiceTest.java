package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceWayangPersistenceServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingStoreValuesResolveDefaultsAndExposeStatus() {
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce")));

        Map<String, Object> values = service.toMap();
        Map<String, Object> capabilities = map(values.get("persistenceCapabilities"));
        Map<String, Object> persistenceTarget = map(values.get("persistenceTarget"));
        Map<String, Object> health = map(values.get("persistenceHealth"));
        Map<String, Object> transferAuditDiagnostics = map(values.get("transferAuditDiagnostics"));
        Map<String, Object> transferAuditStorage = map(transferAuditDiagnostics.get("storage"));

        assertThat(service.loadRuntimeConfig()).isEmpty();
        assertThat(service.loadBootstrapConfig()).isEmpty();
        assertThat(service.runtimeConfigOrDefault().httpConfig().checkoutBasePath())
                .isEqualTo(AgenticCommerceHttpAdapter.DEFAULT_CHECKOUT_BASE_PATH);
        assertThat(service.bootstrapConfigOrDefault().skillIds())
                .containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(values)
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("runtimeConfigAvailable", false)
                .containsEntry("bootstrapConfigAvailable", false)
                .containsEntry("bootstrapReportAvailable", false)
                .containsEntry("manifestAvailable", false)
                .containsKeys(
                        "snapshot",
                        "preflight",
                        "connectorDiagnostics",
                        "transferAuditDiagnostics",
                        "persistenceTarget",
                        "persistenceCapabilities",
                        "persistenceHealth",
                        "runtimeConfig",
                        "bootstrapConfig",
                        "store");
        assertThat(persistenceTarget)
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(capabilities)
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("durable", true)
                .containsEntry("localFile", true);
        assertThat(health)
                .containsEntry("ready", true)
                .containsEntry("complete", false)
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_INCOMPLETE)
                .containsEntry("availableDocumentCount", 0);
        assertThat(map(health.get("summary")))
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_INCOMPLETE)
                .containsEntry("missingDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(values.get("snapshot")))
                .containsEntry("runtimeConfigSource", "default")
                .containsEntry("bootstrapConfigSource", "default")
                .containsKey("persistenceTarget");
        assertThat(map(values.get("preflight")))
                .containsEntry("ready", true)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsKey("persistenceTarget");
        assertThat(map(values.get("connectorDiagnostics")))
                .containsEntry("ready", true)
                .containsEntry("connectorKind", AgenticCommerceConnectorFactoryConfig.CONNECTOR_IN_MEMORY)
                .containsEntry("contractAvailable", false);
        assertThat(transferAuditDiagnostics)
                .containsEntry("ready", true)
                .containsEntry("diagnosticStatus", AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_DEGRADED)
                .containsEntry("contractAvailable", false)
                .containsEntry("auditEnabled", true);
        assertThat(transferAuditStorage)
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY)
                .containsEntry("ephemeral", true);
    }

    @Test
    void buildsRuntimeFromPersistedRuntimeConfig() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        AgenticCommerceWayangRuntimeConfig runtimeConfig = AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl("https://seller.example/")
                        .withAttributes(Map.of("tenant", "demo")))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build())
                .build();
        store.saveRuntimeConfig(runtimeConfig);
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);

        AgenticCommerceWayangRuntime runtime = service.inMemoryRuntime();
        Map<String, Object> status = service.toMap();
        Map<String, Object> statusRuntimeConfig = map(status.get("runtimeConfig"));
        Map<String, Object> connectorConfig = map(statusRuntimeConfig.get("connectorConfig"));

        assertThat(runtime.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(runtime.connectorConfig().bearerToken()).isEqualTo("seller-token");
        assertThat(runtime.httpConfig().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(status).containsEntry("runtimeConfigAvailable", true);
        assertThat(connectorConfig)
                .containsEntry("bearerTokenConfigured", true)
                .doesNotContainKey("bearerToken");
    }

    @Test
    void buildsRuntimeFromConnectorFactoryConfigAndPersistedRuntimeConfig() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl("https://seller.example/"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);

        AgenticCommerceWayangRuntime runtime = service.runtime(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of(
                "mode",
                "memory")));

        assertThat(runtime.connector()).isInstanceOf(InMemoryAgenticCommerceConnector.class);
        assertThat(runtime.connectorConfig().baseUrl()).isEqualTo("https://seller.example");
        assertThat(runtime.connectorConfig().bearerToken()).isEqualTo("seller-token");
        assertThat(runtime.smokeProbe().passed()).isTrue();
    }

    @Test
    void buildsRuntimeFromPersistedConnectorFactoryConfig() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "seller-http")))
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("seller-token")
                        .withBaseUrl("https://seller.example/"))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);

        AgenticCommerceWayangRuntime runtime = service.runtime();

        assertThat(runtime.connector()).isInstanceOf(HttpAgenticCommerceConnector.class);
        assertThat(runtime.connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(runtime.connectorConfig().bearerToken()).isEqualTo("seller-token");
    }

    @Test
    void saveProfilePersistsRuntimeAndBootstrapConfigs() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);
        AgenticCommerceWayangRuntimeProfile profile = AgenticCommerceWayangRuntimeProfile.fromMap(Map.of(
                "profile",
                "production",
                "seller",
                Map.of(
                        "baseUrl",
                        "https://seller.example/",
                        "bearerToken",
                        "seller-token"),
                "bootstrap",
                Map.of("includeRuntimeSkills", false)));

        service.saveProfile(profile);

        assertThat(service.loadRuntimeConfig()).isPresent();
        assertThat(service.loadBootstrapConfig()).isPresent();
        assertThat(service.preflight().ready()).isTrue();
        assertThat(service.runtime().connector()).isInstanceOf(HttpAgenticCommerceConnector.class);
        assertThat(service.runtimeConfigOrDefault().connectorFactoryConfig().connectorKind())
                .isEqualTo(AgenticCommerceConnectorFactoryConfig.CONNECTOR_HTTP);
        assertThat(service.bootstrapConfigOrDefault().includeRuntimeSkills()).isFalse();
    }

    @Test
    void connectorPolicyIsAppliedWhenBuildingRuntimeFromFactoryConfig() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.defaults()
                        .withBaseUrl("http://seller.example"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);
        AgenticCommerceConnectorPolicy policy =
                AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example"));

        assertThatThrownBy(() -> service.runtime(
                AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http")),
                policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connector_https_required");
    }

    @Test
    void persistedConnectorPolicyIsAppliedWhenBuildingRuntimeFromFactoryConfig() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.defaults()
                        .withBaseUrl("http://seller.example"))
                .connectorPolicy(AgenticCommerceConnectorPolicy.strictHosted(List.of("seller.example")))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);

        assertThatThrownBy(() -> service.runtime(AgenticCommerceConnectorFactoryConfig.fromMap(Map.of("mode", "http"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connector_https_required");
    }

    @Test
    void bootstrapAndPersistSavesConfigsReportAndManifest() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce"));
        AgenticCommerceWayangBootstrapConfig bootstrapConfig = AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .build();
        store.saveBootstrapConfig(bootstrapConfig);
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(store);
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory(
                AgenticCommerceWayangRuntimeConfig.builder()
                        .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                                .checkoutBasePath("/commerce/acp")
                                .smokePath("/internal/acp/smoke")
                                .bindingReportPath("/internal/acp/binding")
                                .build())
                        .build());

        AgenticCommerceWayangBootstrapReport report = service.bootstrapAndPersist(
                runtime,
                new AgenticCommerceTestSkillRegistry());
        Map<String, Object> persistedReport = store.loadBootstrapReport().orElseThrow();
        Map<String, Object> persistedManifest = store.loadManifest().orElseThrow();
        Map<String, Object> persistedBootstrapConfig = map(persistedReport.get("bootstrapConfig"));
        Map<String, Object> manifestBootstrapConfig = map(persistedManifest.get("bootstrapConfig"));

        assertThat(report.ready()).isTrue();
        assertThat(report.skillRegistration().definitionCount()).isEqualTo(1);
        assertThat(report.skillRegistration().runtimeSkillCount()).isZero();
        assertThat(store.loadRuntimeConfig().orElseThrow().httpConfig().checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(store.loadBootstrapConfig().orElseThrow().skillIds())
                .containsExactly(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT);
        assertThat(persistedReport).containsEntry("ready", true);
        assertThat(persistedBootstrapConfig).containsEntry("includeRuntimeSkills", false);
        assertThat(manifestBootstrapConfig).containsEntry("includeRuntimeSkills", false);
        assertThat(service.toMap())
                .containsEntry("bootstrapReportAvailable", true)
                .containsEntry("manifestAvailable", true);
    }

    @Test
    void configuredServiceBuildsStoreFromPersistenceConfig() {
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.configured(
                AgenticCommerceWayangPersistenceConfig.file(temporaryDirectory.resolve("configured").toString()));

        assertThat(service.store()).isInstanceOf(FileAgenticCommerceWayangPersistenceStore.class);
        assertThat(service.toMap()).containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void transferAuditDiagnosticsCanRunConfiguredFileContract() throws Exception {
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("agentic-commerce")));
        Path auditJournal = temporaryDirectory.resolve("audit/transfer-audit.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig =
                AgenticCommerceWayangPersistenceTransferAuditConfig.file(auditJournal.toString(), 2);

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                service.transferAuditDiagnostics(auditConfig, true);
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> contract = map(values.get("contract"));
        Map<String, Object> provider = map(values.get("provider"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_HEALTHY);
        assertThat(provider)
                .containsEntry("providerContract", true)
                .containsEntry("providerMatched", true)
                .containsEntry("verifyReload", true);
        assertThat(contract)
                .containsEntry("passed", true)
                .containsEntry("retainedTrailCount", 2)
                .containsEntry("reloadTrailCount", 2);
        assertThat(Files.readAllLines(auditJournal, StandardCharsets.UTF_8)).hasSize(2);
    }

    @Test
    void transferAuditDiagnosticsUsesObjectStoreResolver() {
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(
                InMemoryAgenticCommerceWayangPersistenceStore.create());
        InMemoryAgenticCommerceObjectStoreClient client =
                InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "s3",
                        "bucket",
                        "wayang-audit",
                        "keyPrefix",
                        "service",
                        "journalObject",
                        "audit.jsonl",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                service.transferAuditDiagnostics(auditConfig, client, true);
        Map<String, Object> target = map(diagnostics.toMap().get("target"));
        Map<String, Object> contract = map(diagnostics.toMap().get("contract"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(target)
                .containsEntry("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE)
                .containsEntry("auditObjectKey", "service/audit.jsonl");
        assertThat(contract)
                .containsEntry("passed", true)
                .containsEntry("reloadTrailCount", 2);
        assertThat(client.contains("wayang-audit", "service/audit.jsonl")).isTrue();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
