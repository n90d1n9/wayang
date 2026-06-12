package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs the transfer audit contract against config/provider-built sinks.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness {

    private final AgenticCommerceWayangPersistenceTransferAuditContractHarness contractHarness;
    private final AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers;
    private final AgenticCommerceObjectStoreClientResolver objectStoreClientResolver;
    private final AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver;

    public AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness() {
        this(
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo(),
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults(),
                null,
                null);
    }

    public AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness(
            AgenticCommerceWayangPersistenceTransferAuditContractHarness contractHarness,
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        this.contractHarness = contractHarness == null
                ? AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                : contractHarness;
        this.providers = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        this.objectStoreClientResolver = objectStoreClientResolver;
        this.databasePersistenceClientResolver = databasePersistenceClientResolver;
    }

    public static AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness retainedLatestTwo() {
        return new AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness();
    }

    public static AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness retainedLatestTwo(
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        return new AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness(
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo(),
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults(),
                objectStoreClientResolver,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractHarness contractHarness() {
        return contractHarness;
    }

    public AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers() {
        return providers;
    }

    public AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness withResolvers(
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        return new AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness(
                contractHarness,
                providers,
                objectStoreClientResolver,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness withProviders(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        return new AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness(
                contractHarness,
                providers,
                objectStoreClientResolver,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport run(
            AgenticCommerceWayangPersistenceTransferAuditConfig config) {
        return run(config, false);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport run(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            boolean verifyReload) {
        AgenticCommerceWayangPersistenceTransferAuditConfig resolved = Objects.requireNonNull(config, "config");
        AgenticCommerceWayangPersistenceTransferAuditContractReport report = contractHarness.run(
                () -> resolved.buildSink(
                        providers,
                        objectStoreClientResolver,
                        databasePersistenceClientResolver),
                verifyReload);
        return report.withAttributes(attributes(resolved, verifyReload));
    }

    private Map<String, Object> attributes(
            AgenticCommerceWayangPersistenceTransferAuditConfig config,
            boolean verifyReload) {
        Optional<AgenticCommerceWayangPersistenceTransferAuditStoreProvider> provider = providers.provider(config);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerContract", true);
        values.put("storageKind", config.storageKind());
        values.put("providerMatched", provider.isPresent());
        values.put("providerStorageKind", provider
                .map(AgenticCommerceWayangPersistenceTransferAuditStoreProvider::storageKind)
                .orElse(""));
        values.put("providerStorageKinds", providers.storageKinds());
        values.put("configuredMaxTrails", config.maxTrails());
        values.put("configuredReadable", !config.noopStore());
        values.put("verifyReload", verifyReload);
        values.put("config", config.toMap());
        return Map.copyOf(values);
    }
}
