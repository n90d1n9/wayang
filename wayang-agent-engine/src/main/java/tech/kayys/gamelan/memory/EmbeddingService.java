package tech.kayys.gamelan.memory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.gollek.spi.embedding.EmbeddingResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedding service — wraps the Gollek SDK embedding API with:
 * <ul>
 *   <li><b>LRU cache</b> — avoids re-embedding identical strings</li>
 *   <li><b>Batch embedding</b> — embeds multiple texts in one SDK call</li>
 *   <li><b>Health check</b> — probes the embedding model at startup</li>
 *   <li><b>Graceful degradation</b> — returns empty array on failure
 *       so callers can handle "embedding unavailable" without crashing</li>
 *   <li><b>Normalisation</b> — L2-normalises all output vectors so that
 *       dot-product = cosine-similarity (faster at search time)</li>
 * </ul>
 *
 * <h2>Default model</h2>
 * {@code nomic-embed-text} (768 dimensions) — same model used by the
 * {@link tech.kayys.gamelan.tool.builtin.EmbeddingSearchTool}. Can be
 * overridden via {@code gamelan.embedding.model} config property.
 *
 * <h2>Cache design</h2>
 * Simple LRU backed by a {@code LinkedHashMap} with access order.
 * Max 2 000 entries (~16 MB at 768-float vectors). Cache key is the
 * concatenation of {@code model + "|" + text} to isolate different models.
 */
@ApplicationScoped
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    static final String DEFAULT_MODEL = "nomic-embed-text";

    /** LRU cache capacity — ~16 MB at 768 floats per entry. */
    private static final int CACHE_SIZE = 2_000;

    @Inject GollekSdk sdk;

    private String embeddingModel = DEFAULT_MODEL;
    private boolean available = false;

    /** LRU cache: cacheKey(model,text) → normalised float[] */
    private final Map<String, float[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    @PostConstruct
    void probe() {
        try {
            float[] test = embedDirect(embeddingModel, "health check");
            available = test.length > 0;
            log.info("[embedding] model={} dims={} status={}",
                    embeddingModel, test.length, available ? "OK" : "unavailable");
        } catch (Exception e) {
            available = false;
            log.warn("[embedding] model {} not available: {}", embeddingModel, e.getMessage());
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** @return true if the embedding model responded successfully at startup */
    public boolean isAvailable() { return available; }

    /** @return the active embedding model identifier */
    public String model() { return embeddingModel; }

    /** Override the default embedding model. */
    public void setModel(String model) {
        this.embeddingModel = model;
        cache.clear(); // invalidate cache when model changes
        probe();
    }

    /**
     * Embed a single text. Returns an empty array if embedding is unavailable.
     * Result is L2-normalised and cached.
     *
     * @param text any string to embed (max ~512 tokens for nomic-embed-text)
     * @return normalised embedding vector, or float[0] on failure
     */
    public float[] embed(String text) {
        return embed(embeddingModel, text);
    }

    /**
     * Embed with an explicit model override.
     */
    public float[] embed(String model, String text) {
        if (text == null || text.isBlank()) return new float[0];

        String key = cacheKey(model, text);
        float[] cached = cache.get(key);
        if (cached != null) {
            log.trace("[embedding] cache hit for {} chars", text.length());
            return cached;
        }

        try {
            float[] vec = embedDirect(model, text);
            if (vec.length == 0) return vec;
            float[] normalised = l2Normalise(vec);
            cache.put(key, normalised);
            return normalised;
        } catch (Exception e) {
            log.debug("[embedding] embed failed: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * Batch-embed multiple texts in a single SDK call.
     * Returns an array of float[] in the same order as the input list.
     * Entries that fail are replaced with float[0].
     *
     * @param texts list of texts to embed
     * @return array of embedding vectors (same length as input)
     */
    public float[][] embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new float[0][0];

        // Check cache first, collect misses
        float[][] results = new float[texts.size()][];
        List<Integer> missIndices = new ArrayList<>();
        List<String>  missTexts   = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) {
                results[i] = new float[0];
                continue;
            }
            float[] cached = cache.get(cacheKey(embeddingModel, text));
            if (cached != null) {
                results[i] = cached;
            } else {
                missIndices.add(i);
                missTexts.add(text);
            }
        }

        if (missTexts.isEmpty()) return results;

        // Embed all misses in one SDK call
        try {
            EmbeddingResponse resp = sdk.createEmbedding(
                    EmbeddingRequest.builder()
                            .model(embeddingModel)
                            .inputs(missTexts)   // batch input
                            .build());

            if (resp == null || resp.embeddings() == null) {
                for (int idx : missIndices) results[idx] = new float[0];
                return results;
            }

            List<float[]> embeddings = resp.embeddings();
            for (int j = 0; j < missIndices.size() && j < embeddings.size(); j++) {
                float[] vec = embeddings.get(j);
                if (vec == null) vec = new float[0];
                float[] normalised = vec.length > 0 ? l2Normalise(vec) : vec;
                int originalIdx = missIndices.get(j);
                results[originalIdx] = normalised;
                cache.put(cacheKey(embeddingModel, missTexts.get(j)), normalised);
            }
        } catch (Exception e) {
            log.warn("[embedding] batch embed failed: {}", e.getMessage());
            for (int idx : missIndices) results[idx] = new float[0];
        }

        return results;
    }

    /** Cache size at present. Useful for diagnostics. */
    public int cacheSize() { return cache.size(); }

    /** Invalidate the entire embedding cache. */
    public void clearCache() { cache.clear(); }

    // ── Private ────────────────────────────────────────────────────────────

    private float[] embedDirect(String model, String text) throws SdkException {
        EmbeddingResponse resp = sdk.createEmbedding(
                EmbeddingRequest.builder().model(model).input(text).build());
        if (resp == null || resp.embeddings() == null || resp.embeddings().isEmpty())
            return new float[0];
        float[] vec = resp.embeddings().get(0);
        return vec != null ? vec : new float[0];
    }

    static float[] l2Normalise(float[] v) {
        double sum = 0;
        for (float x : v) sum += (double) x * x;
        if (sum < 1e-12) return v;
        float norm = (float) Math.sqrt(sum);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / norm;
        return out;
    }

    private static String cacheKey(String model, String text) {
        // Keep it short: model + first 200 chars of text
        return model + "|" + (text.length() > 200 ? text.substring(0, 200) : text);
    }
}
