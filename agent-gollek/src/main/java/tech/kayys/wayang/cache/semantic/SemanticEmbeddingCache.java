package tech.kayys.gamelan.cache.semantic;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * SemanticEmbeddingCache — persistent LRU cache for text embeddings.
 *
 * <h2>Why cache embeddings</h2>
 * Embedding generation is the second-most expensive LLM operation after
 * completion. In an agentic system:
 * <ul>
 *   <li>The same file contents are embedded repeatedly across planning, memory,
 *       and search operations in one session</li>
 *   <li>Skill descriptions are embedded every time SkillSelector runs</li>
 *   <li>Episode task descriptions are embedded for relevance lookups</li>
 * </ul>
 * With caching, repeated embeddings cost 0ms instead of 200–500ms each.
 *
 * <h2>Cache design</h2>
 * <ul>
 *   <li><b>Key</b>: SHA-256 of the text content (length + first 200 chars is fast
 *       pre-filter)</li>
 *   <li><b>Value</b>: float[] embedding vector</li>
 *   <li><b>Eviction</b>: LRU with configurable max entries (default 5,000)</li>
 *   <li><b>Persistence</b>: written to {@code ~/.gamelan/cache/embeddings.bin}
 *       on eviction and shutdown, loaded on startup</li>
 *   <li><b>TTL</b>: embeddings for mutable content (files) expire after 24h;
 *       embeddings for static content (skill descriptions) never expire</li>
 * </ul>
 *
 * <h2>Hit rate target</h2>
 * In a typical coding assistant session, the hit rate should exceed 70% because:
 * <ul>
 *   <li>The same 10–20 source files are read and embedded repeatedly</li>
 *   <li>The same 5–15 skill descriptions are embedded on every turn</li>
 *   <li>Memory retrieval queries for "similar episodes" repeat with similar queries</li>
 * </ul>
 */
@ApplicationScoped
public class SemanticEmbeddingCache {

    private static final Logger log = LoggerFactory.getLogger(SemanticEmbeddingCache.class);

    private static final int    MAX_ENTRIES           = 5_000;
    private static final long   FILE_TTL_HOURS        = 24;
    private static final long   STATIC_TTL_HOURS      = 24 * 30; // 30 days for static content

    @Inject GollekSdk    sdk;
    @Inject GamelanConfig config;

    // LRU cache backed by LinkedHashMap
    private final Map<String, CacheEntry>  cache      = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) { // accessOrder=true for LRU
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> e) {
                    if (size() > MAX_ENTRIES) {
                        log.debug("[embed-cache] evicting '{}' (LRU)", e.getKey().substring(0, 8));
                        return true;
                    }
                    return false;
                }
            });

    private final AtomicLong hits        = new AtomicLong();
    private final AtomicLong misses      = new AtomicLong();
    private final AtomicLong computeMs   = new AtomicLong();

    @PostConstruct
    void init() {
        loadFromDisk();
        log.info("[embed-cache] initialized: {} entries loaded", cache.size());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns the embedding for the given text, computing it if not cached.
     *
     * @param text    the text to embed
     * @param ttlType whether the content is mutable (FILE) or static (STATIC)
     * @return the embedding vector
     */
    public float[] embed(String text, TtlType ttlType) {
        if (text == null || text.isBlank()) return new float[0];

        String key = cacheKey(text);
        CacheEntry entry = cache.get(key);

        // Check cache hit (with TTL validation)
        if (entry != null && !entry.isExpired(ttlType == TtlType.STATIC
                ? STATIC_TTL_HOURS : FILE_TTL_HOURS)) {
            hits.incrementAndGet();
            return entry.vector();
        }

        // Cache miss — compute embedding
        misses.incrementAndGet();
        long start = System.currentTimeMillis();
        float[] vector = computeEmbedding(text);
        computeMs.addAndGet(System.currentTimeMillis() - start);

        if (vector.length > 0) {
            cache.put(key, new CacheEntry(key, vector, Instant.now(), text.length()));
        }
        return vector;
    }

    /**
     * Embeds a list of texts in batch, using cached values where available.
     * Uncached texts are embedded concurrently (up to 4 at a time).
     */
    public Map<String, float[]> embedBatch(List<String> texts, TtlType ttlType) {
        Map<String, float[]> results = new ConcurrentHashMap<>();
        List<String> toCompute = new ArrayList<>();

        // Separate cache hits from misses
        for (String text : texts) {
            String key = cacheKey(text);
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired(FILE_TTL_HOURS)) {
                hits.incrementAndGet();
                results.put(text, entry.vector());
            } else {
                toCompute.add(text);
            }
        }

        if (!toCompute.isEmpty()) {
            // Compute missing embeddings with limited concurrency
            Semaphore sem = new Semaphore(4);
            ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
            List<CompletableFuture<Void>> futures = toCompute.stream()
                    .map(text -> CompletableFuture.runAsync(() -> {
                        try {
                            sem.acquire();
                            float[] vec = computeEmbedding(text);
                            if (vec.length > 0) {
                                String key = cacheKey(text);
                                cache.put(key, new CacheEntry(key, vec, Instant.now(), text.length()));
                                results.put(text, vec);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            sem.release();
                        }
                    }, exec))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(e -> null)
                    .join();
            exec.shutdown();
        }

        return Collections.unmodifiableMap(results);
    }

    /**
     * Computes cosine similarity between two texts, using cached embeddings.
     */
    public double similarity(String textA, String textB) {
        float[] a = embed(textA, TtlType.FILE);
        float[] b = embed(textB, TtlType.FILE);
        return cosineSimilarity(a, b);
    }

    /**
     * Finds the top-K most similar texts from a candidate list.
     */
    public List<SimilarityResult> findTopK(String query, List<String> candidates,
                                            int k, TtlType ttlType) {
        float[] queryVec = embed(query, ttlType);
        if (queryVec.length == 0) return List.of();

        return candidates.stream()
                .map(candidate -> {
                    float[] vec = embed(candidate, ttlType);
                    double sim = vec.length > 0 ? cosineSimilarity(queryVec, vec) : 0.0;
                    return new SimilarityResult(candidate, sim);
                })
                .sorted(Comparator.comparingDouble(SimilarityResult::similarity).reversed())
                .limit(k)
                .toList();
    }

    /**
     * Evicts all expired entries from the cache.
     */
    public int evictExpired() {
        int before = cache.size();
        cache.entrySet().removeIf(e -> e.getValue().isExpired(FILE_TTL_HOURS));
        int evicted = before - cache.size();
        if (evicted > 0) log.info("[embed-cache] evicted {} expired entries", evicted);
        return evicted;
    }

    /** Persists the cache to disk. */
    public void persist() {
        saveToDisk();
    }

    public CacheStats stats() {
        long h = hits.get(), m = misses.get();
        long total = h + m;
        return new CacheStats(cache.size(), h, m,
                total == 0 ? 0 : (double) h / total,
                computeMs.get(),
                m == 0 ? 0 : computeMs.get() / m);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private float[] computeEmbedding(String text) {
        try {
            var resp = sdk.createEmbedding(
                    EmbeddingRequest.builder()
                            .model(config.defaultModel())
                            .input(text.length() > 8000 ? text.substring(0, 8000) : text)
                            .build());
            if (resp == null || resp.embeddings() == null || resp.embeddings().isEmpty()) {
                return new float[0];
            }
            float[] result = resp.embeddings().get(0);
            return result != null ? result : new float[0];
        } catch (Exception e) {
            log.debug("[embed-cache] embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0) return 0.0;
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += (double) a[i] * b[i];
            na  += (double) a[i] * a[i];
            nb  += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom < 1e-9 ? 0 : dot / denom;
    }

    private String cacheKey(String text) {
        // Fast key: length prefix + hash of content
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", hash[i]));
            return text.length() + ":" + sb;
        } catch (Exception e) {
            return text.length() + ":" + text.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        Path file = cachePath();
        if (!Files.exists(file)) return;
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            List<CacheEntry> entries = (List<CacheEntry>) in.readObject();
            entries.forEach(e -> cache.put(e.key(), e));
            log.info("[embed-cache] loaded {} entries from disk", entries.size());
        } catch (Exception e) {
            log.warn("[embed-cache] could not load cache from disk: {}", e.getMessage());
        }
    }

    private void saveToDisk() {
        Path file = cachePath();
        try {
            Files.createDirectories(file.getParent());
            List<CacheEntry> toSave = new ArrayList<>(cache.values());
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeObject(toSave);
            }
            log.debug("[embed-cache] saved {} entries to disk", toSave.size());
        } catch (IOException e) {
            log.warn("[embed-cache] could not save cache: {}", e.getMessage());
        }
    }

    private Path cachePath() {
        return Path.of(System.getProperty("user.home"), ".gamelan", "cache", "embeddings.bin");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum TtlType { FILE, STATIC }

    public static final class CacheEntry implements Serializable {
        private final String  key;
        private final float[] vector;
        private final Instant createdAt;
        private final int     textLength;

        CacheEntry(String key, float[] vector, Instant createdAt, int textLength) {
            this.key = key; this.vector = vector;
            this.createdAt = createdAt; this.textLength = textLength;
        }

        boolean isExpired(long ttlHours) {
            return java.time.Duration.between(createdAt, Instant.now()).toHours() > ttlHours;
        }

        String  key()        { return key; }
        float[] vector()     { return vector; }
        Instant createdAt()  { return createdAt; }
        int     textLength() { return textLength; }
    }

    public record SimilarityResult(String text, double similarity) {}

    public record CacheStats(
            int    size,
            long   hits,
            long   misses,
            double hitRate,
            long   totalComputeMs,
            long   avgComputeMsPerMiss
    ) {
        public String summary() {
            return String.format("EmbedCache: %d entries | %.0f%% hit rate | %d hits %d misses | avg=%dms/miss",
                    size, hitRate*100, hits, misses, avgComputeMsPerMiss);
        }
    }
}
