package tech.kayys.wayang.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Produces EmbeddingModuleConfig from MicroProfile config with fallback to defaults.
 */
@ApplicationScoped
public class EmbeddingModuleConfigProducer {

    @Produces
    @ApplicationScoped
    public EmbeddingModuleConfig produce() {
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        Config mpConfig = ConfigProvider.getConfig();

        mpConfig.getOptionalValue("wayang.embedding.default-provider", String.class)
                .filter(value -> !value.isBlank())
                .ifPresent(config::setDefaultProvider);

        mpConfig.getOptionalValue("wayang.embedding.default-model", String.class)
                .filter(value -> !value.isBlank())
                .ifPresent(config::setDefaultModel);

        mpConfig.getOptionalValue("wayang.embedding.version", String.class)
                .filter(value -> !value.isBlank())
                .ifPresent(config::setEmbeddingVersion);

        mpConfig.getOptionalValue("wayang.embedding.normalize", Boolean.class)
                .ifPresent(config::setNormalize);

        mpConfig.getOptionalValue("wayang.embedding.cache.enabled", Boolean.class)
                .ifPresent(config::setCacheEnabled);

        mpConfig.getOptionalValue("wayang.embedding.cache.max-entries", Integer.class)
                .ifPresent(config::setCacheMaxEntries);

        mpConfig.getOptionalValue("wayang.embedding.batch.size", Integer.class)
                .ifPresent(config::setBatchSize);

        mpConfig.getOptionalValue("wayang.embedding.batch.queue-capacity", Integer.class)
                .ifPresent(config::setBatchQueueCapacity);

        mpConfig.getOptionalValue("wayang.embedding.batch.max-retries", Integer.class)
                .ifPresent(config::setBatchMaxRetries);

        mpConfig.getOptionalValue("wayang.embedding.batch.worker-threads", Integer.class)
                .ifPresent(config::setBatchWorkerThreads);

        mpConfig.getOptionalValue("wayang.embedding.tenant-strategies", String.class)
                .filter(value -> !value.isBlank())
                .ifPresent(config::loadTenantStrategies);

        return config;
    }
}
