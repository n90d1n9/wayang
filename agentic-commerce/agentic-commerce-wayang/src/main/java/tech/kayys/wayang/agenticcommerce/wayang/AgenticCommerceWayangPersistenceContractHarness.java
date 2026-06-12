package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Store contract harness for Agentic Commerce Wayang persistence adapters.
 */
public final class AgenticCommerceWayangPersistenceContractHarness {

    public static final String CONTRACT_ID = "agentic-commerce-wayang-persistence-round-trip";
    public static final String CONTRACT_BASE_URL = "https://contract-seller.example";
    public static final String CONTRACT_BEARER_TOKEN = "contract-seller-token";

    private final AgenticCommerceWayangRuntimeConfig runtimeConfig;
    private final AgenticCommerceWayangBootstrapConfig bootstrapConfig;

    public AgenticCommerceWayangPersistenceContractHarness() {
        this(contractRuntimeConfig(), contractBootstrapConfig());
    }

    public AgenticCommerceWayangPersistenceContractHarness(
            AgenticCommerceWayangRuntimeConfig runtimeConfig,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        this.runtimeConfig = runtimeConfig == null ? contractRuntimeConfig() : runtimeConfig;
        this.bootstrapConfig = bootstrapConfig == null ? contractBootstrapConfig() : bootstrapConfig;
    }

    public static AgenticCommerceWayangPersistenceContractHarness roundTrip() {
        return new AgenticCommerceWayangPersistenceContractHarness();
    }

    public AgenticCommerceWayangRuntimeConfig runtimeConfig() {
        return runtimeConfig;
    }

    public AgenticCommerceWayangBootstrapConfig bootstrapConfig() {
        return bootstrapConfig;
    }

    public AgenticCommerceWayangPersistenceContractReport run(AgenticCommerceWayangPersistenceStore store) {
        AgenticCommerceWayangPersistenceStore resolved = Objects.requireNonNull(store, "store");
        List<String> issues = new ArrayList<>();
        Map<String, Object> statusBefore = status(resolved, issues, "store_status_before_failed");
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory(runtimeConfig);
        AgenticCommerceWayangBootstrapReport bootstrapReport = bootstrapReport(runtime);
        AgenticCommerceWayangManifest manifest = runtime.manifest(bootstrapConfig);

        saveRuntimeConfig(resolved, issues);
        saveBootstrapConfig(resolved, issues);
        saveBootstrapReport(resolved, bootstrapReport, issues);
        saveManifest(resolved, manifest, issues);

        Optional<AgenticCommerceWayangRuntimeConfig> loadedRuntimeConfig = loadRuntimeConfig(resolved, issues);
        Optional<AgenticCommerceWayangBootstrapConfig> loadedBootstrapConfig = loadBootstrapConfig(resolved, issues);
        Optional<Map<String, Object>> loadedBootstrapReport = loadBootstrapReport(resolved, issues);
        Optional<Map<String, Object>> loadedManifest = loadManifest(resolved, issues);

        validateRuntimeConfig(loadedRuntimeConfig, issues);
        validateBootstrapConfig(loadedBootstrapConfig, issues);
        validateBootstrapReport(loadedBootstrapReport, issues);
        validateManifest(loadedManifest, issues);

        Map<String, Object> statusAfter = status(resolved, issues, "store_status_after_failed");
        return new AgenticCommerceWayangPersistenceContractReport(
                resolved.storageKind(),
                runtimeConfig,
                bootstrapConfig,
                loadedRuntimeConfig.isPresent(),
                loadedBootstrapConfig.isPresent(),
                loadedBootstrapReport.isPresent(),
                loadedManifest.isPresent(),
                issues,
                statusBefore,
                statusAfter,
                loadedBootstrapReport.orElseGet(Map::of),
                loadedManifest.orElseGet(Map::of),
                attributes());
    }

    private void saveRuntimeConfig(AgenticCommerceWayangPersistenceStore store, List<String> issues) {
        try {
            store.saveRuntimeConfig(runtimeConfig);
        } catch (RuntimeException exception) {
            issues.add("runtime_config_save_failed");
        }
    }

    private void saveBootstrapConfig(AgenticCommerceWayangPersistenceStore store, List<String> issues) {
        try {
            store.saveBootstrapConfig(bootstrapConfig);
        } catch (RuntimeException exception) {
            issues.add("bootstrap_config_save_failed");
        }
    }

    private void saveBootstrapReport(
            AgenticCommerceWayangPersistenceStore store,
            AgenticCommerceWayangBootstrapReport bootstrapReport,
            List<String> issues) {
        try {
            store.saveBootstrapReport(bootstrapReport);
        } catch (RuntimeException exception) {
            issues.add("bootstrap_report_save_failed");
        }
    }

    private void saveManifest(
            AgenticCommerceWayangPersistenceStore store,
            AgenticCommerceWayangManifest manifest,
            List<String> issues) {
        try {
            store.saveManifest(manifest);
        } catch (RuntimeException exception) {
            issues.add("manifest_save_failed");
        }
    }

    private Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadRuntimeConfig();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadBootstrapConfig();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> loadBootstrapReport(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadBootstrapReport();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.loadFailureIssue());
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> loadManifest(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        try {
            return store.loadManifest();
        } catch (RuntimeException exception) {
            issues.add(AgenticCommerceWayangPersistenceDocuments.MANIFEST.loadFailureIssue());
            return Optional.empty();
        }
    }

    private void validateRuntimeConfig(
            Optional<AgenticCommerceWayangRuntimeConfig> loadedRuntimeConfig,
            List<String> issues) {
        if (loadedRuntimeConfig.isEmpty()) {
            issues.add("runtime_config_missing_after_save");
            return;
        }
        AgenticCommerceWayangRuntimeConfig loaded = loadedRuntimeConfig.orElseThrow();
        if (!runtimeConfig.connectorFactoryConfig().connectorKind()
                .equals(loaded.connectorFactoryConfig().connectorKind())) {
            issues.add("runtime_connector_factory_mismatch");
        }
        if (!runtimeConfig.connectorConfig().baseUrl().equals(loaded.connectorConfig().baseUrl())) {
            issues.add("runtime_connector_base_url_mismatch");
        }
        if (!runtimeConfig.connectorConfig().bearerToken().equals(loaded.connectorConfig().bearerToken())) {
            issues.add("runtime_connector_token_mismatch");
        }
        if (!runtimeConfig.httpConfig().checkoutBasePath().equals(loaded.httpConfig().checkoutBasePath())) {
            issues.add("runtime_http_config_mismatch");
        }
    }

    private void validateBootstrapConfig(
            Optional<AgenticCommerceWayangBootstrapConfig> loadedBootstrapConfig,
            List<String> issues) {
        if (loadedBootstrapConfig.isEmpty()) {
            issues.add("bootstrap_config_missing_after_save");
            return;
        }
        AgenticCommerceWayangBootstrapConfig loaded = loadedBootstrapConfig.orElseThrow();
        if (!bootstrapConfig.skillIds().equals(loaded.skillIds())) {
            issues.add("bootstrap_skill_ids_mismatch");
        }
        if (bootstrapConfig.includeDefinitions() != loaded.includeDefinitions()
                || bootstrapConfig.includeRuntimeSkills() != loaded.includeRuntimeSkills()) {
            issues.add("bootstrap_install_mode_mismatch");
        }
    }

    private void validateBootstrapReport(
            Optional<Map<String, Object>> loadedBootstrapReport,
            List<String> issues) {
        if (loadedBootstrapReport.isEmpty()) {
            issues.add("bootstrap_report_missing_after_save");
            return;
        }
        Map<String, Object> report = loadedBootstrapReport.orElseThrow();
        if (!Boolean.TRUE.equals(report.get("ready"))) {
            issues.add("bootstrap_report_not_ready");
        }
        if (number(report.get("issueCount")) != 0) {
            issues.add("bootstrap_report_issue_count_nonzero");
        }
    }

    private void validateManifest(Optional<Map<String, Object>> loadedManifest, List<String> issues) {
        if (loadedManifest.isEmpty()) {
            issues.add("manifest_missing_after_save");
            return;
        }
        Map<String, Object> manifest = loadedManifest.orElseThrow();
        if (number(manifest.get("skillCount")) != AgenticCommerceWayang.checkoutSkillIds().size()) {
            issues.add("manifest_skill_count_mismatch");
        }
        if (number(manifest.get("routeCount")) == 0) {
            issues.add("manifest_route_count_missing");
        }
    }

    private AgenticCommerceWayangBootstrapReport bootstrapReport(AgenticCommerceWayangRuntime runtime) {
        return AgenticCommerceWayangBootstrapReport.from(
                runtime,
                successfulRegistration(),
                bootstrapConfig);
    }

    private AgenticCommerceSkillRegistration successfulRegistration() {
        return new AgenticCommerceSkillRegistration(
                bootstrapConfig.skillIds(),
                bootstrapConfig.includeDefinitions() ? bootstrapConfig.skillIds() : List.of(),
                bootstrapConfig.includeRuntimeSkills() ? bootstrapConfig.skillIds() : List.of(),
                List.of(),
                Map.of("contractId", CONTRACT_ID));
    }

    private Map<String, Object> status(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues,
            String issue) {
        try {
            return store.toMap();
        } catch (RuntimeException exception) {
            issues.add(issue);
            return Map.of();
        }
    }

    private Map<String, Object> attributes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contractId", CONTRACT_ID);
        values.put("documentCount", AgenticCommerceWayangPersistenceDocuments.count());
        values.put("runtimeSecretRoundTripRequired", true);
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangRuntimeConfig contractRuntimeConfig() {
        return AgenticCommerceWayangRuntimeConfig.builder()
                .connectorFactoryConfig(AgenticCommerceConnectorFactoryConfig.inMemory())
                .connectorConfig(AgenticCommerceConnectorConfig.bearer(CONTRACT_BEARER_TOKEN)
                        .withBaseUrl(CONTRACT_BASE_URL)
                        .withHeaders(Map.of("X-Contract", "persistence"))
                        .withAttributes(Map.of("contract", CONTRACT_ID)))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/contract/agentic-commerce")
                        .smokePath("/contract/agentic-commerce/smoke")
                        .bindingReportPath("/contract/agentic-commerce/binding")
                        .build())
                .build();
    }

    private static AgenticCommerceWayangBootstrapConfig contractBootstrapConfig() {
        return AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(
                        AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                        AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT))
                .includeRuntimeSkills(false)
                .requireSkillRegistration(true)
                .requireSmokeProbe(true)
                .requireBindingRoutes(true)
                .build();
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
