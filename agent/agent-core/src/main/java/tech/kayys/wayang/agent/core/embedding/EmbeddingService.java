package tech.kayys.wayang.agent.core.embedding;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.gollek.spi.embedding.EmbeddingResponse;
import tech.kayys.gollek.sdk.core.GollekSdk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating text embedding vectors using the Gollek inference engine.
 *
 * <p>Embeddings are used by {@link tech.kayys.wayang.agent.core.memory.pgvector.PgVectorMemory}
 * and {@link tech.kayys.wayang.agent.core.skills.builtin.RAGSkill} to enable semantic
 * search over stored facts and retrieved documents.</p>
 *
 * <h2>Caching</h2>
 * Results are cached in a bounded LRU cache to avoid re-embedding the same
 * text repeatedly within a session. The cache is bounded to 1000 entries by
 * evicting the oldest entry when full (simple FIFO approximation via
 * {@code LinkedHashMap} with {@code accessOrder=true}).
 *
 * <h2>Batch embedding</h2>
 * Multiple texts can be embedded in a single call via the {@link #embedBatch}
 * method. The inference engine is asked to embed all texts in one request,
 * which is more efficient than N individual calls.
 *
 * <h2>Dimensions</h2>
 * The output dimension depends on the embedding model. Common dimensions:
 * <ul>
 *   <li>768 — BERT-base, MiniLM-L6, nomic-embed-text</li>
 *   <li>1024 — BERT-large, multilingual-e5-large</li>
 *   <li>1536 — OpenAI text-embedding-3-small</li>
 *   <li>3072 — OpenAI text-embedding-3-large</li>
 * </ul>
 * Configure via {@code gollek.agent.memory.vector-dimensions}.
 */
@ApplicationScoped
public class EmbeddingService {

    private static final Logger LOG = Logger.getLogger(EmbeddingService.class);
    private static final int CACHE_MAX = 1000;

    @Inject GollekSdk gollekSdk;

    /** Simple LRU cache: text → float[]. */
    @SuppressWarnings("serial")
    private final Map<String, float[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, float[]>(CACHE_MAX + 1, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, float[]> e) {
                    return size() > CACHE_MAX;
                }
            });

    // ── Single embedding ──────────────────────────────────────────────────────

    /**
     * Embed a single text string.
     *
     * @param text  text to embed (must not be blank)
     * @param model embedding model id (pass {@code null} to use the default)
     * @return float[] vector; empty array on failure
     */
    public Uni<float[]> embed(String text, String model) {
        if (text == null || text.isBlank()) {
            return Uni.createFrom().item(new float[0]);
        }

        // Check cache first
        float[] cached = cache.get(text);
        if (cached != null) return Uni.createFrom().item(cached);

        String effectiveModel = model != null && !model.isBlank() ? model : "default";

        EmbeddingRequest req = EmbeddingRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .model(effectiveModel)
                .input(text)
                .build();

        return Uni.createFrom().item(() -> {
            try {
                return gollekSdk.createEmbedding(req);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
                .map(resp -> {
                    if (resp.embeddings() == null || resp.embeddings().isEmpty()) {
                        LOG.warnf("EmbeddingService: got empty embeddings for model=%s", effectiveModel);
                        return new float[0];
                    }
                    float[] vec = resp.embeddings().get(0);
                    cache.put(text, vec);
                    return vec;
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("EmbeddingService: embedding failed for model=%s: %s", effectiveModel, err.getMessage());
                    return new float[0];
                });
    }

    /** Embed with the default model. */
    public Uni<float[]> embed(String text) {
        return embed(text, null);
    }

    // ── Batch embedding ───────────────────────────────────────────────────────

    /**
     * Embed multiple texts in a single inference call.
     *
     * @param texts list of texts to embed
     * @param model embedding model id (null = default)
     * @return list of vectors in the same order as {@code texts}
     */
    public Uni<List<float[]>> embedBatch(List<String> texts, String model) {
        if (texts == null || texts.isEmpty()) return Uni.createFrom().item(List.of());

        String effectiveModel = model != null && !model.isBlank() ? model : "default";

        // Split into: already-cached and need-embedding
        Map<Integer, float[]> results = new ConcurrentHashMap<>();
        List<Integer> toEmbed    = new ArrayList<>();
        List<String>  textsToEmbed = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            float[] cached = t != null ? cache.get(t) : null;
            if (cached != null) {
                results.put(i, cached);
            } else {
                toEmbed.add(i);
                textsToEmbed.add(t != null ? t : "");
            }
        }

        if (textsToEmbed.isEmpty()) {
            // All from cache
            return Uni.createFrom().item(buildOrderedList(texts.size(), results));
        }

        EmbeddingRequest req = EmbeddingRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .model(effectiveModel)
                .inputs(textsToEmbed)
                .build();

        return Uni.createFrom().item(() -> {
            try {
                return gollekSdk.createEmbedding(req);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
                .map(resp -> {
                    List<float[]> vecs = resp.embeddings() != null ? resp.embeddings() : List.of();
                    for (int j = 0; j < toEmbed.size(); j++) {
                        float[] vec = j < vecs.size() ? vecs.get(j) : new float[0];
                        results.put(toEmbed.get(j), vec);
                        String origText = textsToEmbed.get(j);
                        if (vec.length > 0) cache.put(origText, vec);
                    }
                    return buildOrderedList(texts.size(), results);
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("EmbeddingService batch failed: %s", err.getMessage());
                    List<float[]> fallback = new ArrayList<>();
                    for (int i = 0; i < texts.size(); i++)
                        fallback.add(results.getOrDefault(i, new float[0]));
                    return fallback;
                });
    }

    // ── Cosine similarity ─────────────────────────────────────────────────────

    /**
     * Compute cosine similarity between two vectors.
     * Returns 0.0 if either vector is empty.
     *
     * @return similarity in [0, 1] (1 = identical direction)
     */
    public static float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length)
            return 0f;
        double dot  = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    /**
     * Re-rank a list of texts by cosine similarity to a query vector.
     * Returns the {@code topK} most similar, in descending similarity order.
     */
    public Uni<List<ScoredText>> rerank(String query, List<String> candidates, int topK, String model) {
        return embed(query, model)
                .chain(queryVec -> embedBatch(candidates, model)
                        .map(candVecs -> {
                            List<ScoredText> scored = new ArrayList<>();
                            for (int i = 0; i < candidates.size(); i++) {
                                float sim = cosineSimilarity(queryVec, candVecs.get(i));
                                scored.add(new ScoredText(candidates.get(i), sim));
                            }
                            scored.sort(Comparator.comparingDouble(ScoredText::score).reversed());
                            return scored.stream().limit(topK).toList();
                        }));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<float[]> buildOrderedList(int size, Map<Integer, float[]> results) {
        List<float[]> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(results.getOrDefault(i, new float[0]));
        return list;
    }

    /** A text with its cosine similarity score. */
    public record ScoredText(String text, float score) {}
}
