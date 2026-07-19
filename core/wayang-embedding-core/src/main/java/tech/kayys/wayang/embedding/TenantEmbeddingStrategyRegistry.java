package tech.kayys.wayang.embedding;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TenantEmbeddingStrategyRegistry {

    private final Map<String, TenantEmbeddingStrategy> strategies = new ConcurrentHashMap<>();

    public void register(String tenantId, String provider, String model) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        strategies.put(tenantId, new TenantEmbeddingStrategy(provider, model));
    }

    public Optional<TenantEmbeddingStrategy> find(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(tenantId));
    }

    public Map<String, TenantEmbeddingStrategy> snapshot() {
        return Map.copyOf(strategies);
    }

    public record TenantEmbeddingStrategy(String provider, String model) {
    }
}
