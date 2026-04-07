package tech.kayys.wayang.rag.embedding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.embedding.EmbeddingConfigRuntime;
import tech.kayys.wayang.embedding.EmbeddingModuleConfig;
import tech.kayys.wayang.embedding.TenantEmbeddingStrategyRegistry;

import java.time.Instant;
import java.util.Map;

/**
 * Service for managing embedding configuration at runtime.
 * Provides capabilities to check current configuration status and trigger
 * reloads
 * of tenant-specific embedding strategies.
 */
@ApplicationScoped
public class EmbeddingConfigAdminService {

    @Inject
    EmbeddingConfigRuntime runtime;

    public EmbeddingConfigStatus status() {
        return toStatus(runtime.current());
    }

    public EmbeddingConfigStatus reload() {
        runtime.reload();
        return toStatus(runtime.current());
    }

    private EmbeddingConfigStatus toStatus(EmbeddingModuleConfig config) {
        Map<String, EmbeddingConfigStatus.TenantEmbeddingStrategyStatus> tenantStrategies = config.tenantStrategies()
                .snapshot().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> toTenantStatus(entry.getValue())));

        return new EmbeddingConfigStatus(
                config.getDefaultProvider(),
                config.getDefaultModel(),
                config.getEmbeddingVersion(),
                config.isNormalize(),
                tenantStrategies,
                Instant.now());
    }

    private EmbeddingConfigStatus.TenantEmbeddingStrategyStatus toTenantStatus(
            TenantEmbeddingStrategyRegistry.TenantEmbeddingStrategy strategy) {
        return new EmbeddingConfigStatus.TenantEmbeddingStrategyStatus(
                strategy.provider(),
                strategy.model());
    }
}
