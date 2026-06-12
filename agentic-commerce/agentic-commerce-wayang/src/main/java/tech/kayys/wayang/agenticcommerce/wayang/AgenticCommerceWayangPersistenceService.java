package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Operator-facing facade over Agentic Commerce Wayang persistence.
 */
public final class AgenticCommerceWayangPersistenceService {

    private final AgenticCommerceWayangPersistenceStore store;

    public AgenticCommerceWayangPersistenceService(AgenticCommerceWayangPersistenceStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public static AgenticCommerceWayangPersistenceService of(AgenticCommerceWayangPersistenceStore store) {
        return new AgenticCommerceWayangPersistenceService(store);
    }

    public static AgenticCommerceWayangPersistenceService configured(
            AgenticCommerceWayangPersistenceConfig persistenceConfig) {
        AgenticCommerceWayangPersistenceConfig config = persistenceConfig == null
                ? AgenticCommerceWayangPersistenceConfig.defaults()
                : persistenceConfig;
        return of(config.buildStore());
    }

    public static AgenticCommerceWayangPersistenceService configured(
            AgenticCommerceWayangPersistenceConfig persistenceConfig,
            AgenticCommerceObjectStoreClient objectStoreClient) {
        return configured(persistenceConfig, AgenticCommerceObjectStoreClientResolver.fixed(objectStoreClient));
    }

    public static AgenticCommerceWayangPersistenceService configured(
            AgenticCommerceWayangPersistenceConfig persistenceConfig,
            AgenticCommerceDatabasePersistenceClient databasePersistenceClient) {
        return configured(
                persistenceConfig,
                AgenticCommerceDatabasePersistenceClientResolver.fixed(databasePersistenceClient));
    }

    public static AgenticCommerceWayangPersistenceService configured(
            AgenticCommerceWayangPersistenceConfig persistenceConfig,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        AgenticCommerceWayangPersistenceConfig config = persistenceConfig == null
                ? AgenticCommerceWayangPersistenceConfig.defaults()
                : persistenceConfig;
        return of(config.buildStore(objectStoreClientResolver));
    }

    public static AgenticCommerceWayangPersistenceService configured(
            AgenticCommerceWayangPersistenceConfig persistenceConfig,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        AgenticCommerceWayangPersistenceConfig config = persistenceConfig == null
                ? AgenticCommerceWayangPersistenceConfig.defaults()
                : persistenceConfig;
        return of(config.buildStore(databasePersistenceClientResolver));
    }

    public AgenticCommerceWayangPersistenceStore store() {
        return store;
    }

    public AgenticCommerceWayangPersistenceCapabilities capabilities() {
        return AgenticCommerceWayangPersistenceCapabilities.from(store);
    }

    public AgenticCommerceWayangPersistenceHealthReport persistenceHealth() {
        return AgenticCommerceWayangPersistenceHealthReport.from(store);
    }

    public AgenticCommerceWayangConfigSnapshot snapshot() {
        return AgenticCommerceWayangConfigSnapshot.from(store);
    }

    public AgenticCommerceWayangConfigReloadReport reload(AgenticCommerceWayangConfigSnapshot previousSnapshot) {
        return AgenticCommerceWayangConfigReloadReport.from(previousSnapshot, snapshot());
    }

    public AgenticCommerceRuntimePreflightReport preflight() {
        return snapshot().preflight();
    }

    public AgenticCommerceWayangPersistenceTransferPreflightReport preflightTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore) {
        return preflightTransferTo(targetStore, AgenticCommerceWayangPersistenceTransferOptions.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferPreflightReport preflightTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        return AgenticCommerceWayangPersistenceTransferPreflightReport.from(
                store,
                Objects.requireNonNull(targetStore, "targetStore"),
                options);
    }

    public AgenticCommerceWayangPersistenceTransferPreflightReport preflightTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        return preflightTransferTo(
                targetStore,
                AgenticCommerceWayangPersistenceTransferOptions.defaults(),
                auditSink);
    }

    public AgenticCommerceWayangPersistenceTransferPreflightReport preflightTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        AgenticCommerceWayangPersistenceTransferPreflightReport report =
                preflightTransferTo(targetStore, options);
        recordAudit(auditSink, report);
        return report;
    }

    public AgenticCommerceConnectorContractReport connectorContract() {
        return runtime().connectorContract();
    }

    public AgenticCommerceWayangPersistenceContractReport persistenceContract() {
        return AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);
    }

    public AgenticCommerceWayangPersistenceTransferPlan planTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore) {
        return planTransferTo(targetStore, AgenticCommerceWayangPersistenceTransferOptions.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferPlan planTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        return AgenticCommerceWayangPersistenceTransfer.configured(options).plan(
                store,
                Objects.requireNonNull(targetStore, "targetStore"));
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore) {
        return applyTransferTo(targetStore, AgenticCommerceWayangPersistenceTransferOptions.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        return applyTransferTo(targetStore, options, false);
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options,
            boolean force) {
        AgenticCommerceWayangPersistenceStore target = Objects.requireNonNull(targetStore, "targetStore");
        AgenticCommerceWayangPersistenceTransferOptions transferOptions = options == null
                ? AgenticCommerceWayangPersistenceTransferOptions.defaults()
                : options;
        AgenticCommerceWayangPersistenceTransferPreflightReport preflight =
                preflightTransferTo(target, transferOptions);
        if (!force && !preflight.readyToApply()) {
            return AgenticCommerceWayangPersistenceTransferApplyReport.blocked(preflight);
        }
        AgenticCommerceWayangPersistenceTransferReport transferReport =
                transferTo(target, transferOptions);
        return AgenticCommerceWayangPersistenceTransferApplyReport.applied(preflight, transferReport, force);
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        return applyTransferTo(
                targetStore,
                AgenticCommerceWayangPersistenceTransferOptions.defaults(),
                false,
                auditSink);
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        return applyTransferTo(targetStore, options, false, auditSink);
    }

    public AgenticCommerceWayangPersistenceTransferApplyReport applyTransferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options,
            boolean force,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        AgenticCommerceWayangPersistenceTransferApplyReport report =
                applyTransferTo(targetStore, options, force);
        recordAudit(auditSink, report);
        return report;
    }

    public AgenticCommerceWayangPersistenceTransferReport transferTo(
            AgenticCommerceWayangPersistenceStore targetStore) {
        return transferTo(targetStore, AgenticCommerceWayangPersistenceTransferOptions.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferReport transferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        return AgenticCommerceWayangPersistenceTransfer.configured(options).copy(
                store,
                Objects.requireNonNull(targetStore, "targetStore"));
    }

    public AgenticCommerceWayangPersistenceTransferReport transferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        return transferTo(
                targetStore,
                AgenticCommerceWayangPersistenceTransferOptions.defaults(),
                auditSink);
    }

    public AgenticCommerceWayangPersistenceTransferReport transferTo(
            AgenticCommerceWayangPersistenceStore targetStore,
            AgenticCommerceWayangPersistenceTransferOptions options,
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink) {
        AgenticCommerceWayangPersistenceTransferReport report = transferTo(targetStore, options);
        recordAudit(auditSink, report);
        return report;
    }

    public AgenticCommerceConnectorDiagnostics connectorDiagnostics() {
        return snapshot().connectorDiagnostics();
    }

    public AgenticCommerceConnectorDiagnostics connectorDiagnostics(boolean includeContract) {
        AgenticCommerceWayangConfigSnapshot snapshot = snapshot();
        if (!includeContract) {
            return snapshot.connectorDiagnostics();
        }
        return AgenticCommerceConnectorDiagnostics.from(snapshot, snapshot.buildRuntime().connectorContract());
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics() {
        return transferAuditDiagnostics(AgenticCommerceWayangPersistenceTransferAuditConfig.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig) {
        return AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(resolveAuditConfig(auditConfig));
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig,
            boolean includeContract) {
        return transferAuditDiagnostics(auditConfig, null, null, includeContract, includeContract);
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig,
            AgenticCommerceObjectStoreClient objectStoreClient,
            boolean includeContract) {
        return transferAuditDiagnostics(
                auditConfig,
                objectStoreClient == null ? null : AgenticCommerceObjectStoreClientResolver.fixed(objectStoreClient),
                null,
                includeContract,
                includeContract);
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig,
            AgenticCommerceDatabasePersistenceClient databasePersistenceClient,
            boolean includeContract) {
        return transferAuditDiagnostics(
                auditConfig,
                null,
                databasePersistenceClient == null
                        ? null
                        : AgenticCommerceDatabasePersistenceClientResolver.fixed(databasePersistenceClient),
                includeContract,
                includeContract);
    }

    public AgenticCommerceWayangPersistenceTransferAuditDiagnostics transferAuditDiagnostics(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver,
            boolean includeContract,
            boolean verifyReload) {
        AgenticCommerceWayangPersistenceTransferAuditConfig config = resolveAuditConfig(auditConfig);
        if (!includeContract) {
            return AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(config);
        }
        return AgenticCommerceWayangPersistenceTransferAuditDiagnostics.check(
                config,
                objectStoreClientResolver,
                databasePersistenceClientResolver,
                verifyReload);
    }

    public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
        return store.loadRuntimeConfig();
    }

    public AgenticCommerceWayangRuntimeConfig runtimeConfigOrDefault() {
        return loadRuntimeConfig().orElseGet(AgenticCommerceWayangRuntimeConfig::defaults);
    }

    public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        store.saveRuntimeConfig(runtimeConfig);
    }

    public void saveProfile(AgenticCommerceWayangRuntimeProfile profile) {
        AgenticCommerceWayangRuntimeProfile resolved = profile == null
                ? AgenticCommerceWayangRuntimeProfile.local()
                : profile;
        store.saveRuntimeConfig(resolved.runtimeConfig());
        store.saveBootstrapConfig(resolved.bootstrapConfig());
    }

    public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
        return store.loadBootstrapConfig();
    }

    public AgenticCommerceWayangBootstrapConfig bootstrapConfigOrDefault() {
        return loadBootstrapConfig().orElseGet(AgenticCommerceWayangBootstrapConfig::defaults);
    }

    public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        store.saveBootstrapConfig(bootstrapConfig);
    }

    public Optional<Map<String, Object>> loadBootstrapReport() {
        return store.loadBootstrapReport();
    }

    public Optional<Map<String, Object>> loadManifest() {
        return store.loadManifest();
    }

    public AgenticCommerceWayangRuntime runtime(AgenticCommerceConnector connector) {
        return snapshot().buildRuntime(Objects.requireNonNull(connector, "connector"));
    }

    public AgenticCommerceWayangRuntime runtime() {
        return snapshot().buildRuntime();
    }

    public AgenticCommerceWayangRuntime runtime(AgenticCommerceWayangRuntimeProfile profile) {
        AgenticCommerceWayangRuntimeProfile resolved = profile == null
                ? AgenticCommerceWayangRuntimeProfile.local()
                : profile;
        return resolved.runtime();
    }

    public AgenticCommerceWayangRuntime runtime(AgenticCommerceConnectorFactoryConfig connectorFactoryConfig) {
        return snapshot().buildRuntime(connectorFactoryConfig);
    }

    public AgenticCommerceWayangRuntime runtime(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorPolicy connectorPolicy) {
        return snapshot().buildRuntime(connectorFactoryConfig, connectorPolicy);
    }

    public AgenticCommerceWayangRuntime inMemoryRuntime() {
        return runtime(AgenticCommerceConnectorFactoryConfig.inMemory());
    }

    public AgenticCommerceWayangManifest manifest(AgenticCommerceWayangRuntime runtime) {
        return manifest(runtime, bootstrapConfigOrDefault());
    }

    public AgenticCommerceWayangManifest manifest(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangBootstrapConfig config = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        return Objects.requireNonNull(runtime, "runtime").manifest(config);
    }

    public AgenticCommerceWayangManifest persistManifest(AgenticCommerceWayangRuntime runtime) {
        return persistManifest(runtime, bootstrapConfigOrDefault());
    }

    public AgenticCommerceWayangManifest persistManifest(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangManifest manifest = manifest(runtime, bootstrapConfig);
        store.saveManifest(manifest);
        return manifest;
    }

    public AgenticCommerceWayangBootstrapReport bootstrapAndPersist(
            AgenticCommerceWayangRuntime runtime,
            SkillRegistry registry) {
        return bootstrapAndPersist(runtime, registry, bootstrapConfigOrDefault());
    }

    public AgenticCommerceWayangBootstrapReport bootstrapAndPersist(
            AgenticCommerceWayangRuntime runtime,
            SkillRegistry registry,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangRuntime resolvedRuntime = Objects.requireNonNull(runtime, "runtime");
        AgenticCommerceWayangBootstrapConfig config = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        AgenticCommerceWayangBootstrapReport report = resolvedRuntime.bootstrap(registry, config);
        persist(resolvedRuntime, config, report);
        return report;
    }

    public void persist(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig,
            AgenticCommerceWayangBootstrapReport bootstrapReport) {
        AgenticCommerceWayangRuntime resolvedRuntime = Objects.requireNonNull(runtime, "runtime");
        AgenticCommerceWayangBootstrapConfig config = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        AgenticCommerceWayangBootstrapReport report = Objects.requireNonNull(bootstrapReport, "bootstrapReport");
        store.saveRuntimeConfig(resolvedRuntime.runtimeConfig());
        store.saveBootstrapConfig(config);
        store.saveBootstrapReport(report);
        store.saveManifest(resolvedRuntime.manifest(config));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", store.storageKind());
        values.put("store", store.toMap());
        values.put("persistenceTarget", snapshot().persistenceTarget());
        values.put("persistenceCapabilities", capabilities().toMap());
        values.put("persistenceHealth", persistenceHealth().toMap());
        values.put("runtimeConfigAvailable", loadRuntimeConfig().isPresent());
        values.put("bootstrapConfigAvailable", loadBootstrapConfig().isPresent());
        values.put("bootstrapReportAvailable", loadBootstrapReport().isPresent());
        values.put("manifestAvailable", loadManifest().isPresent());
        values.put("snapshot", snapshot().toMap());
        values.put("preflight", preflight().toMap());
        values.put("connectorDiagnostics", connectorDiagnostics().toMap());
        values.put("transferAuditDiagnostics", transferAuditDiagnostics().toMap());
        values.put("runtimeConfig", runtimeConfigOrDefault().toMap());
        values.put("bootstrapConfig", bootstrapConfigOrDefault().toMap());
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditConfig resolveAuditConfig(
            AgenticCommerceWayangPersistenceTransferAuditConfig auditConfig) {
        return auditConfig == null ? AgenticCommerceWayangPersistenceTransferAuditConfig.defaults() : auditConfig;
    }

    private static void recordAudit(
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink,
            AgenticCommerceWayangPersistenceTransferPreflightReport report) {
        if (auditSink != null) {
            auditSink.record(report);
        }
    }

    private static void recordAudit(
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink,
            AgenticCommerceWayangPersistenceTransferReport report) {
        if (auditSink != null) {
            auditSink.record(report);
        }
    }

    private static void recordAudit(
            AgenticCommerceWayangPersistenceTransferAuditSink auditSink,
            AgenticCommerceWayangPersistenceTransferApplyReport report) {
        if (auditSink != null) {
            auditSink.record(report);
        }
    }
}
