package tech.kayys.wayang.memory.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.memory.spi.EmbeddingService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI-based embedding service for generating embeddings using GPT models.
 * 
 * Supports OpenAI's embedding models (text-embedding-3-small, text-embedding-3-large).
 * Embeddings are cached to improve performance and reduce API costs.
 * 
 * Configuration:
 * - gamelan.embedding.openai.api-key: OpenAI API key
 * - gamelan.embedding.openai.model: Model to use (default: text-embedding-3-small)
 * - gamelan.embedding.openai.timeout-secs: Request timeout in seconds (default: 30)
 * - gamelan.embedding.openai.cache-enabled: Enable embedding cache (default: true)
 */
@ApplicationScoped
public class OpenAIEmbeddingService implements EmbeddingService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIEmbeddingService.class);

    @ConfigProperty(name = "gamelan.embedding.openai.api-key")
    String apiKey;

    @ConfigProperty(name = "gamelan.embedding.openai.model", defaultValue = "text-embedding-3-small")
    String model;

    @ConfigProperty(name = "gamelan.embedding.openai.timeout-secs", defaultValue = "30")
    int timeoutSeconds;

    @ConfigProperty(name = "gamelan.embedding.openai.cache-enabled", defaultValue = "true")
    boolean cacheEnabled;

    /**
     * Cache for embeddings: key = text, value = embedding vector
     */
    private final ConcurrentHashMap<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();

    /**
     * Embed a single text string and return as List<Float>
     * 
     * @param text The text to embed
     * @return Uni containing the embedding vector as a List<Float>
     */
    @Override
    public Uni<List<Float>> embed(String text) {
        if (text == null || text.isBlank()) {
            LOG.warn("Attempted to embed empty or null text");
            return Uni.createFrom().item(Collections.emptyList());
        }

        // Check cache first
        if (cacheEnabled && embeddingCache.containsKey(text)) {
            LOG.debug("Embedding cache hit for text (length: {})", text.length());
            return Uni.createFrom().item(embeddingCache.get(text));
        }

        // Call OpenAI API
        return callOpenAIEmbeddingAPI(text)
                .onItem().invoke(embedding -> {
                    if (cacheEnabled) {
                        embeddingCache.put(text, embedding);
                        LOG.debug("Cached embedding for text (length: {})", text.length());
                    }
                })
                .onFailure().invoke(ex -> {
                    LOG.error("Failed to embed text using OpenAI API", ex);
                });
    }

    /**
     * Embed multiple texts and return as List<List<Float>>
     * 
     * @param texts The texts to embed
     * @return Uni containing list of embedding vectors
     */
    public Uni<List<List<Float>>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            LOG.warn("Attempted to embed empty batch");
            return Uni.createFrom().item(Collections.emptyList());
        }

        // For now, embed one by one
        // TODO: Implement true batch API call for efficiency
        return Uni.join()
                .all(texts.stream()
                        .map(this::embed)
                        .toList())
                .andCollectFailures()
                .replaceWith(
                        texts.stream()
                                .map(this::getCachedOrEmbed)
                                .toList());
    }

    /**
     * Clear the embedding cache
     */
    public Uni<Void> clearCache() {
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> {
                    embeddingCache.clear();
                    LOG.info("Embedding cache cleared");
                });
    }

    /**
     * Get cache statistics
     */
    public Uni<String> getCacheStats() {
        return Uni.createFrom().item(() -> {
            int cacheSize = embeddingCache.size();
            return String.format("OpenAI Embedding Cache Stats: size=%d, model=%s, cache-enabled=%s",
                    cacheSize, model, cacheEnabled);
        });
    }

    /**
     * Call the actual OpenAI Embeddings API
     * 
     * This is a placeholder for the actual HTTP call to OpenAI.
     * Production implementation would use RestClient or similar.
     * 
     * @param text The text to embed
     * @return Uni containing the embedding vector
     */
    private Uni<List<Float>> callOpenAIEmbeddingAPI(String text) {
        // TODO: Implement actual OpenAI API call
        // return restClient.post("https://api.openai.com/v1/embeddings", request)
        //        .withHeader("Authorization", "Bearer " + apiKey)
        //        .sendAndAwait()
        //        .thenApply(response -> parseEmbeddingResponse(response));

        // For now, return a placeholder
        LOG.warn("OpenAI API call not yet implemented - using placeholder");
        return Uni.createFrom().item(generatePlaceholderEmbedding(text));
    }

    /**
     * Generate a placeholder embedding for development/testing
     */
    private List<Float> generatePlaceholderEmbedding(String text) {
        // TODO: Remove when actual API is implemented
        // For now, return a 1536-dimensional vector (matches text-embedding-3-large)
        List<Float> embedding = new java.util.ArrayList<>();
        int seed = text.hashCode();
        for (int i = 0; i < 1536; i++) {
            embedding.add((float) Math.sin(seed + i) * 0.5f + 0.5f);
        }
        return embedding;
    }

    /**
     * Helper method to get embedding from cache or compute it
     */
    private List<Float> getCachedOrEmbed(String text) {
        if (cacheEnabled && embeddingCache.containsKey(text)) {
            return embeddingCache.get(text);
        }
        // This would normally be async, but for simplicity returning from cache
        return embeddingCache.getOrDefault(text, generatePlaceholderEmbedding(text));
    }
}
