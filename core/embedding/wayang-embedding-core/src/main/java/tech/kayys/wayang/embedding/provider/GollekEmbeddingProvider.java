package tech.kayys.wayang.embedding.provider;

import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * High-performance embedding provider using local Gollek (Llama.cpp/GGUF).
 */
@ApplicationScoped
public class GollekEmbeddingProvider implements EmbeddingProvider {

    private final GollekSdk sdk;

    @Inject
    public GollekEmbeddingProvider(GollekSdk sdk) {
        this.sdk = sdk;
    }

    @Override
    public String name() {
        return "gollek";
    }

    @Override
    public boolean supports(String model) {
        // Broadly support local GGUF models via Gollek
        return model != null && !model.isBlank();
    }

    @Override
    public List<float[]> embedAll(List<String> inputs, String model) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }

        EmbeddingRequest request = new EmbeddingRequest(
                UUID.randomUUID().toString(),
                model,
                inputs,
                Collections.emptyMap());

        try {
            tech.kayys.gollek.spi.embedding.EmbeddingResponse response = sdk.createEmbedding(request);
            return response.embeddings();
        } catch (tech.kayys.gollek.sdk.exception.SdkException e) {
            throw new tech.kayys.wayang.embedding.EmbeddingException("Gollek embedding failed: " + e.getMessage());
        }
    }
}
