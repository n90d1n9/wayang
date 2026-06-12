package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ephemeral persistence store for embedded runtimes, previews, and contract tests.
 */
public final class InMemoryAgenticCommerceWayangPersistenceStore implements AgenticCommerceWayangPersistenceStore {

    public static final String STORAGE_KIND = "in-memory";

    private Map<String, Object> runtimeConfig;
    private Map<String, Object> bootstrapConfig;
    private Map<String, Object> bootstrapReport;
    private Map<String, Object> manifest;

    public static InMemoryAgenticCommerceWayangPersistenceStore create() {
        return new InMemoryAgenticCommerceWayangPersistenceStore();
    }

    @Override
    public String storageKind() {
        return STORAGE_KIND;
    }

    @Override
    public synchronized Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
        return Optional.ofNullable(runtimeConfig)
                .map(AgenticCommerceWayangMaps::copy)
                .map(AgenticCommerceWayangRuntimeConfig::fromMap);
    }

    @Override
    public synchronized void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        AgenticCommerceWayangRuntimeConfig resolved = runtimeConfig == null
                ? AgenticCommerceWayangRuntimeConfig.defaults()
                : runtimeConfig;
        this.runtimeConfig = AgenticCommerceWayangMaps.copy(resolved.toStorageMap());
    }

    @Override
    public synchronized Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
        return Optional.ofNullable(bootstrapConfig)
                .map(AgenticCommerceWayangMaps::copy)
                .map(AgenticCommerceWayangBootstrapConfig::fromMap);
    }

    @Override
    public synchronized void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangBootstrapConfig resolved = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        this.bootstrapConfig = AgenticCommerceWayangMaps.copy(resolved.toMap());
    }

    @Override
    public synchronized Optional<Map<String, Object>> loadBootstrapReport() {
        return Optional.ofNullable(bootstrapReport).map(AgenticCommerceWayangMaps::copy);
    }

    @Override
    public synchronized void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
        this.bootstrapReport = AgenticCommerceWayangMaps.copy(
                java.util.Objects.requireNonNull(bootstrapReport, "bootstrapReport").toMap());
    }

    @Override
    public synchronized Optional<Map<String, Object>> loadManifest() {
        return Optional.ofNullable(manifest).map(AgenticCommerceWayangMaps::copy);
    }

    @Override
    public synchronized void saveManifest(AgenticCommerceWayangManifest manifest) {
        this.manifest = AgenticCommerceWayangMaps.copy(
                java.util.Objects.requireNonNull(manifest, "manifest").toMap());
    }

    public synchronized void clear() {
        runtimeConfig = null;
        bootstrapConfig = null;
        bootstrapReport = null;
        manifest = null;
    }

    @Override
    public synchronized Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind());
        values.put("ephemeral", true);
        values.put("target", AgenticCommerceWayangPersistenceTargetDescriptor.fromStore(this).toMap());
        values.put("runtimeConfigAvailable", runtimeConfig != null);
        values.put("bootstrapConfigAvailable", bootstrapConfig != null);
        values.put("bootstrapReportAvailable", bootstrapReport != null);
        values.put("manifestAvailable", manifest != null);
        values.put("documentCount", documentCount());
        return Map.copyOf(values);
    }

    private int documentCount() {
        int count = 0;
        count += runtimeConfig == null ? 0 : 1;
        count += bootstrapConfig == null ? 0 : 1;
        count += bootstrapReport == null ? 0 : 1;
        count += manifest == null ? 0 : 1;
        return count;
    }
}
