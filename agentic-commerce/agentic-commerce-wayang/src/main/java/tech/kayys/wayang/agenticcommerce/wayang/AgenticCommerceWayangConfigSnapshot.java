package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Effective Agentic Commerce Wayang configuration loaded from persistence.
 */
public record AgenticCommerceWayangConfigSnapshot(
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceWayangBootstrapConfig bootstrapConfig,
        boolean runtimeConfigPersisted,
        boolean bootstrapConfigPersisted,
        Map<String, Object> storeStatus) {

    public AgenticCommerceWayangConfigSnapshot {
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        bootstrapConfig = bootstrapConfig == null ? AgenticCommerceWayangBootstrapConfig.defaults() : bootstrapConfig;
        storeStatus = AgenticCommerceWayangMaps.copy(storeStatus);
    }

    public static AgenticCommerceWayangConfigSnapshot from(AgenticCommerceWayangPersistenceStore store) {
        AgenticCommerceWayangPersistenceStore resolved = Objects.requireNonNull(store, "store");
        Optional<AgenticCommerceWayangRuntimeConfig> runtimeConfig = resolved.loadRuntimeConfig();
        Optional<AgenticCommerceWayangBootstrapConfig> bootstrapConfig = resolved.loadBootstrapConfig();
        return new AgenticCommerceWayangConfigSnapshot(
                runtimeConfig.orElseGet(AgenticCommerceWayangRuntimeConfig::defaults),
                bootstrapConfig.orElseGet(AgenticCommerceWayangBootstrapConfig::defaults),
                runtimeConfig.isPresent(),
                bootstrapConfig.isPresent(),
                resolved.toMap());
    }

    public String runtimeConfigSource() {
        return runtimeConfigPersisted ? "persisted" : "default";
    }

    public String bootstrapConfigSource() {
        return bootstrapConfigPersisted ? "persisted" : "default";
    }

    public AgenticCommerceWayangRuntime buildRuntime(AgenticCommerceConnector connector) {
        return runtimeConfig.build(Objects.requireNonNull(connector, "connector"));
    }

    public AgenticCommerceWayangRuntime buildRuntime() {
        return buildRuntime(runtimeConfig.connectorFactoryConfig(), runtimeConfig.connectorPolicy());
    }

    public AgenticCommerceWayangRuntime buildRuntime(AgenticCommerceConnectorFactoryConfig connectorFactoryConfig) {
        return buildRuntime(connectorFactoryConfig, runtimeConfig.connectorPolicy());
    }

    public AgenticCommerceWayangRuntime buildRuntime(
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorPolicy connectorPolicy) {
        AgenticCommerceConnectorFactoryConfig config = connectorFactoryConfig == null
                ? runtimeConfig.connectorFactoryConfig()
                : connectorFactoryConfig;
        AgenticCommerceConnectorPolicy policy = connectorPolicy == null
                ? runtimeConfig.connectorPolicy()
                : connectorPolicy;
        AgenticCommerceWayangRuntimeConfig effectiveRuntimeConfig = runtimeConfig
                .withConnectorFactoryConfig(config)
                .withConnectorPolicy(policy);
        return effectiveRuntimeConfig.build();
    }

    public AgenticCommerceWayangRuntime buildInMemoryRuntime() {
        return buildRuntime(AgenticCommerceConnectorFactoryConfig.inMemory());
    }

    public AgenticCommerceRuntimePreflightReport preflight() {
        return AgenticCommerceRuntimePreflightReport.from(this);
    }

    public AgenticCommerceConnectorDiagnostics connectorDiagnostics() {
        return AgenticCommerceConnectorDiagnostics.from(this);
    }

    public Map<String, Object> persistenceTarget() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(
                storeStatus,
                AgenticCommerceWayangMaps.text(storeStatus.get("storageKind")));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runtimeConfigPersisted", runtimeConfigPersisted);
        values.put("bootstrapConfigPersisted", bootstrapConfigPersisted);
        values.put("runtimeConfigSource", runtimeConfigSource());
        values.put("bootstrapConfigSource", bootstrapConfigSource());
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("store", storeStatus);
        values.put("persistenceTarget", persistenceTarget());
        values.put("persistenceCapabilities", AgenticCommerceWayangPersistenceCapabilities.fromStatus(storeStatus).toMap());
        values.put("connectorDiagnostics", connectorDiagnostics().toMap());
        return Map.copyOf(values);
    }
}
