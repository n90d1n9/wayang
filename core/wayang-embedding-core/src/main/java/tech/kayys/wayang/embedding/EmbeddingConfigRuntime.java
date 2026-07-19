package tech.kayys.wayang.embedding;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Holds a reloadable embedding configuration snapshot backed by MicroProfile
 * config.
 */
@ApplicationScoped
public class EmbeddingConfigRuntime {

    private volatile EmbeddingModuleConfig current;
    private volatile String fingerprint;

    @PostConstruct
    void init() {
        current = new EmbeddingModuleConfig();
        fingerprint = "";
        refreshIfChanged();
    }

    public EmbeddingModuleConfig current() {
        refreshIfChanged();
        return current;
    }

    public synchronized void reload() {
        fingerprint = "";
        refreshIfChanged();
    }

    private synchronized void refreshIfChanged() {
        Config config = ConfigProvider.getConfig();
        String defaultProvider = read(config, "wayang.embedding.default-provider", "gollek");
        String defaultModel = read(config, "wayang.embedding.default-model", "Qwen/Qwen2.5-0.5B-Instruct");
        String version = read(config, "wayang.embedding.version", "v1");
        String normalize = read(config, "wayang.embedding.normalize", "true");
        String cacheEnabled = read(config, "wayang.embedding.cache.enabled", "true");
        String cacheMaxEntries = read(config, "wayang.embedding.cache.max-entries", "10000");
        String batchSize = read(config, "wayang.embedding.batch.size", "32");
        String batchQueueCapacity = read(config, "wayang.embedding.batch.queue-capacity", "256");
        String batchMaxRetries = read(config, "wayang.embedding.batch.max-retries", "2");
        String batchWorkerThreads = read(config, "wayang.embedding.batch.worker-threads", "2");
        String tenantStrategies = read(config, "wayang.embedding.tenant-strategies", "");

        String nextFingerprint = defaultProvider + "|" + defaultModel + "|" + version + "|" + normalize + "|"
                + cacheEnabled + "|" + cacheMaxEntries + "|" + batchSize + "|" + batchQueueCapacity + "|"
                + batchMaxRetries + "|" + batchWorkerThreads + "|" + tenantStrategies;
        if (nextFingerprint.equals(fingerprint)) {
            return;
        }

        EmbeddingModuleConfig next = new EmbeddingModuleConfig();
        next.setDefaultProvider(defaultProvider);
        next.setDefaultModel(defaultModel);
        next.setEmbeddingVersion(version);
        next.setNormalize(Boolean.parseBoolean(normalize));
        next.setCacheEnabled(Boolean.parseBoolean(cacheEnabled));
        next.setCacheMaxEntries(parseInt(cacheMaxEntries, 10000));
        next.setBatchSize(parseInt(batchSize, 32));
        next.setBatchQueueCapacity(parseInt(batchQueueCapacity, 256));
        next.setBatchMaxRetries(parseInt(batchMaxRetries, 2));
        next.setBatchWorkerThreads(parseInt(batchWorkerThreads, 2));
        next.loadTenantStrategies(tenantStrategies);

        current = next;
        fingerprint = nextFingerprint;
    }

    private static String read(Config config, String key, String fallback) {
        return config.getOptionalValue(key, String.class)
                .filter(value -> !value.isBlank())
                .orElse(fallback);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
