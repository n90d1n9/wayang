package tech.kayys.wayang.embedding;

public class EmbeddingModuleConfig {

    private String defaultProvider = "gollek";
    private String defaultModel = "Qwen/Qwen2.5-0.5B-Instruct";
    private String embeddingVersion = "v1";
    private boolean normalize = true;
    private boolean cacheEnabled = true;
    private int cacheMaxEntries = 10000;
    private int batchSize = 32;
    private int batchQueueCapacity = 256;
    private int batchMaxRetries = 2;
    private int batchWorkerThreads = 2;
    private final TenantEmbeddingStrategyRegistry tenantStrategies = new TenantEmbeddingStrategyRegistry();

    public EmbeddingModuleConfig() {
        applyExternalOverrides();
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public boolean isNormalize() {
        return normalize;
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheMaxEntries() {
        return cacheMaxEntries;
    }

    public void setCacheMaxEntries(int cacheMaxEntries) {
        this.cacheMaxEntries = cacheMaxEntries;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchQueueCapacity() {
        return batchQueueCapacity;
    }

    public void setBatchQueueCapacity(int batchQueueCapacity) {
        this.batchQueueCapacity = batchQueueCapacity;
    }

    public int getBatchMaxRetries() {
        return batchMaxRetries;
    }

    public void setBatchMaxRetries(int batchMaxRetries) {
        this.batchMaxRetries = batchMaxRetries;
    }

    public int getBatchWorkerThreads() {
        return batchWorkerThreads;
    }

    public void setBatchWorkerThreads(int batchWorkerThreads) {
        this.batchWorkerThreads = batchWorkerThreads;
    }

    public TenantEmbeddingStrategyRegistry tenantStrategies() {
        return tenantStrategies;
    }

    public void setTenantStrategy(String tenantId, String provider, String model) {
        tenantStrategies.register(tenantId, provider, model);
    }

    public void loadTenantStrategies(String spec) {
        if (spec == null || spec.isBlank()) {
            return;
        }

        String[] entries = spec.split(";");
        for (String entry : entries) {
            String item = entry == null ? "" : entry.trim();
            if (item.isEmpty()) {
                continue;
            }

            if (item.contains("|")) {
                String[] parts = item.split("\\|", -1);
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid tenant strategy format: " + item);
                }
                registerParsed(parts[0], parts[1], parts[2], item);
                continue;
            }

            int tenantSeparator = item.indexOf('=');
            int modelSeparator = item.indexOf(':');
            if (tenantSeparator < 1 || modelSeparator < tenantSeparator + 2 || modelSeparator == item.length() - 1) {
                throw new IllegalArgumentException("Invalid tenant strategy format: " + item);
            }

            String tenantId = item.substring(0, tenantSeparator);
            String provider = item.substring(tenantSeparator + 1, modelSeparator);
            String model = item.substring(modelSeparator + 1);
            registerParsed(tenantId, provider, model, item);
        }
    }

    private void applyExternalOverrides() {
        String provider = read("wayang.embedding.default-provider", "WAYANG_EMBEDDING_DEFAULT_PROVIDER");
        if (provider != null && !provider.isBlank()) {
            defaultProvider = provider.trim();
        }

        String model = read("wayang.embedding.default-model", "WAYANG_EMBEDDING_DEFAULT_MODEL");
        if (model != null && !model.isBlank()) {
            defaultModel = model.trim();
        }

        String version = read("wayang.embedding.version", "WAYANG_EMBEDDING_VERSION");
        if (version != null && !version.isBlank()) {
            embeddingVersion = version.trim();
        }

        String normalizeValue = read("wayang.embedding.normalize", "WAYANG_EMBEDDING_NORMALIZE");
        if (normalizeValue != null && !normalizeValue.isBlank()) {
            normalize = Boolean.parseBoolean(normalizeValue.trim());
        }

        String cacheEnabledValue = read("wayang.embedding.cache.enabled", "WAYANG_EMBEDDING_CACHE_ENABLED");
        if (cacheEnabledValue != null && !cacheEnabledValue.isBlank()) {
            cacheEnabled = Boolean.parseBoolean(cacheEnabledValue.trim());
        }

        String cacheMaxEntriesValue = read("wayang.embedding.cache.max-entries", "WAYANG_EMBEDDING_CACHE_MAX_ENTRIES");
        if (cacheMaxEntriesValue != null && !cacheMaxEntriesValue.isBlank()) {
            cacheMaxEntries = parseInt(cacheMaxEntriesValue.trim(), cacheMaxEntries);
        }

        String batchSizeValue = read("wayang.embedding.batch.size", "WAYANG_EMBEDDING_BATCH_SIZE");
        if (batchSizeValue != null && !batchSizeValue.isBlank()) {
            batchSize = parseInt(batchSizeValue.trim(), batchSize);
        }

        String batchQueueCapacityValue = read("wayang.embedding.batch.queue-capacity",
                "WAYANG_EMBEDDING_BATCH_QUEUE_CAPACITY");
        if (batchQueueCapacityValue != null && !batchQueueCapacityValue.isBlank()) {
            batchQueueCapacity = parseInt(batchQueueCapacityValue.trim(), batchQueueCapacity);
        }

        String batchMaxRetriesValue = read("wayang.embedding.batch.max-retries", "WAYANG_EMBEDDING_BATCH_MAX_RETRIES");
        if (batchMaxRetriesValue != null && !batchMaxRetriesValue.isBlank()) {
            batchMaxRetries = parseInt(batchMaxRetriesValue.trim(), batchMaxRetries);
        }

        String batchWorkerThreadsValue = read("wayang.embedding.batch.worker-threads",
                "WAYANG_EMBEDDING_BATCH_WORKER_THREADS");
        if (batchWorkerThreadsValue != null && !batchWorkerThreadsValue.isBlank()) {
            batchWorkerThreads = parseInt(batchWorkerThreadsValue.trim(), batchWorkerThreads);
        }

        String strategySpec = read("wayang.embedding.tenant-strategies", "WAYANG_EMBEDDING_TENANT_STRATEGIES");
        loadTenantStrategies(strategySpec);

        enforceBounds();
    }

    private static String read(String propertyKey, String envKey) {
        String fromProperty = System.getProperty(propertyKey);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return null;
    }

    private void registerParsed(String tenantId, String provider, String model, String rawEntry) {
        String tenant = tenantId == null ? "" : tenantId.trim();
        String prov = provider == null ? "" : provider.trim();
        String mod = model == null ? "" : model.trim();
        if (tenant.isEmpty() || prov.isEmpty() || mod.isEmpty()) {
            throw new IllegalArgumentException("Invalid tenant strategy entry: " + rawEntry);
        }
        tenantStrategies.register(tenant, prov, mod);
    }

    private void enforceBounds() {
        if (cacheMaxEntries <= 0) {
            cacheMaxEntries = 10000;
        }
        if (batchSize <= 0) {
            batchSize = 32;
        }
        if (batchQueueCapacity <= 0) {
            batchQueueCapacity = 256;
        }
        if (batchMaxRetries < 0) {
            batchMaxRetries = 2;
        }
        if (batchWorkerThreads <= 0) {
            batchWorkerThreads = 2;
        }
        if (embeddingVersion == null || embeddingVersion.isBlank()) {
            embeddingVersion = "v1";
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
