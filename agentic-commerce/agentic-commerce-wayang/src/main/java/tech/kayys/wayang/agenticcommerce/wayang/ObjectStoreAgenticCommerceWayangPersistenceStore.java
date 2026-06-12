package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-storage backed persistence for S3/RustFS-compatible stores.
 */
public final class ObjectStoreAgenticCommerceWayangPersistenceStore implements AgenticCommerceWayangPersistenceStore {

    public static final String STORAGE_KIND = "object-store";
    public static final String RUNTIME_CONFIG_OBJECT =
            AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.fileName();
    public static final String BOOTSTRAP_CONFIG_OBJECT =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.fileName();
    public static final String BOOTSTRAP_REPORT_OBJECT =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.fileName();
    public static final String MANIFEST_OBJECT =
            AgenticCommerceWayangPersistenceDocuments.MANIFEST.fileName();

    private final AgenticCommerceObjectStoreConfig config;
    private final AgenticCommerceObjectStoreClient client;

    public ObjectStoreAgenticCommerceWayangPersistenceStore(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client) {
        this.config = Objects.requireNonNull(config, "config");
        this.client = Objects.requireNonNull(client, "client");
    }

    public static ObjectStoreAgenticCommerceWayangPersistenceStore configured(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client) {
        return new ObjectStoreAgenticCommerceWayangPersistenceStore(config, client);
    }

    public AgenticCommerceObjectStoreConfig config() {
        return config;
    }

    public AgenticCommerceObjectStoreClient client() {
        return client;
    }

    public String runtimeConfigKey() {
        return config.objectKey(RUNTIME_CONFIG_OBJECT);
    }

    public String bootstrapConfigKey() {
        return config.objectKey(BOOTSTRAP_CONFIG_OBJECT);
    }

    public String bootstrapReportKey() {
        return config.objectKey(BOOTSTRAP_REPORT_OBJECT);
    }

    public String manifestKey() {
        return config.objectKey(MANIFEST_OBJECT);
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
        values.put("objectStore", config.toMap());
        values.put("target", AgenticCommerceWayangPersistenceTargetDescriptor.fromStore(this).toMap());
        values.put("client", client.toMap());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.objectKeyStatusKey(),
                runtimeConfigKey());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.objectKeyStatusKey(),
                bootstrapConfigKey());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.objectKeyStatusKey(),
                bootstrapReportKey());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.MANIFEST.objectKeyStatusKey(),
                manifestKey());
        values.put("availabilityChecked", false);
        return Map.copyOf(values);
    }

    private Optional<Map<String, Object>> readObject(String key) {
        return client.readText(config.bucket(), key)
                .filter(body -> !body.isBlank())
                .map(AgenticCommerceJson::readObject);
    }

    private void writeObject(String key, Map<String, Object> values) {
        client.writeText(
                config.bucket(),
                key,
                AgenticCommerceProtocol.MIME_JSON,
                AgenticCommerceJson.write(values));
    }
}
