package tech.kayys.wayang.embedding;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmbeddingProviderRegistry {

    private final Map<String, EmbeddingProvider> providers;

    public EmbeddingProviderRegistry(Collection<EmbeddingProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new EmbeddingException("No embedding providers registered");
        }
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(EmbeddingProvider::name, provider -> provider, (a, b) -> b));
    }

    public EmbeddingProvider required(String providerName) {
        EmbeddingProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new EmbeddingException("Embedding provider not found: " + providerName);
        }
        return provider;
    }

    public List<String> names() {
        return providers.keySet().stream().sorted().toList();
    }

    public Optional<EmbeddingProvider> findByModel(String model) {
        if (model == null || model.isBlank()) {
            return Optional.empty();
        }
        return providers.values().stream()
                .filter(provider -> provider.supports(model))
                .findFirst();
    }
}
