package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Database-backed persistence for Agentic Commerce Wayang state documents.
 */
public final class DatabaseAgenticCommerceWayangPersistenceStore implements AgenticCommerceWayangPersistenceStore {

    public static final String STORAGE_KIND = "database";
    public static final String RUNTIME_CONFIG_DOCUMENT =
            AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.fileName();
    public static final String BOOTSTRAP_CONFIG_DOCUMENT =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.fileName();
    public static final String BOOTSTRAP_REPORT_DOCUMENT =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.fileName();
    public static final String MANIFEST_DOCUMENT =
            AgenticCommerceWayangPersistenceDocuments.MANIFEST.fileName();

    private final AgenticCommerceDatabasePersistenceConfig config;
    private final AgenticCommerceDatabasePersistenceClient client;

    public DatabaseAgenticCommerceWayangPersistenceStore(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client) {
        this.config = config == null ? AgenticCommerceDatabasePersistenceConfig.defaults() : config;
        this.client = Objects.requireNonNull(client, "client");
    }

    public static DatabaseAgenticCommerceWayangPersistenceStore configured(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client) {
        return new DatabaseAgenticCommerceWayangPersistenceStore(config, client);
    }

    public AgenticCommerceDatabasePersistenceConfig config() {
        return config;
    }

    public AgenticCommerceDatabasePersistenceClient client() {
        return client;
    }

    public String runtimeConfigKey() {
        return config.documentKey(RUNTIME_CONFIG_DOCUMENT);
    }

    public String bootstrapConfigKey() {
        return config.documentKey(BOOTSTRAP_CONFIG_DOCUMENT);
    }

    public String bootstrapReportKey() {
        return config.documentKey(BOOTSTRAP_REPORT_DOCUMENT);
    }

    public String manifestKey() {
        return config.documentKey(MANIFEST_DOCUMENT);
    }

    @Override
    public String storageKind() {
        return STORAGE_KIND;
    }

    @Override
    public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
        return readObject(runtimeConfigKey()).map(AgenticCommerceWayangRuntimeConfig::fromMap);
    }

    @Override
    public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        AgenticCommerceWayangRuntimeConfig resolved = runtimeConfig == null
                ? AgenticCommerceWayangRuntimeConfig.defaults()
                : runtimeConfig;
        writeObject(runtimeConfigKey(), resolved.toStorageMap());
    }

    @Override
    public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
        return readObject(bootstrapConfigKey()).map(AgenticCommerceWayangBootstrapConfig::fromMap);
    }

    @Override
    public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangBootstrapConfig resolved = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        writeObject(bootstrapConfigKey(), resolved.toMap());
    }

    @Override
    public Optional<Map<String, Object>> loadBootstrapReport() {
        return readObject(bootstrapReportKey());
    }

    @Override
    public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
        writeObject(
                bootstrapReportKey(),
                Objects.requireNonNull(bootstrapReport, "bootstrapReport").toMap());
    }

    @Override
    public Optional<Map<String, Object>> loadManifest() {
        return readObject(manifestKey());
    }

    @Override
    public void saveManifest(AgenticCommerceWayangManifest manifest) {
        writeObject(
                manifestKey(),
                Objects.requireNonNull(manifest, "manifest").toMap());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind());
        values.put("database", config.toMap());
        values.put("target", AgenticCommerceWayangPersistenceTargetDescriptor.fromStore(this).toMap());
        values.put("client", client.toMap());
        values.put("runtimeConfigKey", runtimeConfigKey());
        values.put("bootstrapConfigKey", bootstrapConfigKey());
        values.put("bootstrapReportKey", bootstrapReportKey());
        values.put("manifestKey", manifestKey());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.availabilityStatusKey(),
                client.contains(config.tableName(), runtimeConfigKey()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.availabilityStatusKey(),
                client.contains(config.tableName(), bootstrapConfigKey()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.availabilityStatusKey(),
                client.contains(config.tableName(), bootstrapReportKey()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.MANIFEST.availabilityStatusKey(),
                client.contains(config.tableName(), manifestKey()));
        return Map.copyOf(values);
    }

    private Optional<Map<String, Object>> readObject(String key) {
        return client.readText(config.tableName(), key)
                .filter(body -> !body.isBlank())
                .map(AgenticCommerceJson::readObject);
    }

    private void writeObject(String key, Map<String, Object> values) {
        client.writeText(
                config.tableName(),
                key,
                AgenticCommerceProtocol.MIME_JSON,
                AgenticCommerceJson.write(values));
    }
}
