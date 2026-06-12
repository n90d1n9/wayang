package tech.kayys.gamelan.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Persistent vector repository — the core data structure for semantic memory.
 *
 * <h2>Design</h2>
 * Each entry is a {@link VectorEntry} containing:
 * <ul>
 *   <li>A unique ID</li>
 *   <li>The original text that was embedded</li>
 *   <li>The float[] embedding vector</li>
 *   <li>Arbitrary metadata (type, source, tags)</li>
 *   <li>A timestamp</li>
 * </ul>
 *
 * <h2>Search strategy</h2>
 * Brute-force cosine similarity over the full index. This is correct and
 * fast for collections up to ~100 000 entries (typical for a developer's
 * per-project memory). At that scale the latency is &lt;5 ms on modern
 * hardware. HNSW/FAISS would be needed beyond 1M entries.
 *
 * <h2>Thread safety</h2>
 * A {@link ReadWriteLock} guards the in-memory index. Reads (search) are
 * concurrent; writes (upsert/delete) are exclusive. Persistence is async
 * on a single background thread to avoid blocking the agent loop.
 *
 * <h2>Persistence</h2>
 * Stored as NDJSON (one entry per line) in {@code ~/.gamelan/memory/<ns>.vec}.
 * NDJSON allows efficient append-only writes during a session and complete
 * rewrite on compact/eviction.
 *
 * <h2>Deduplication</h2>
 * Entries are deduplicated by ID. Calling {@link #upsert} with an existing
 * ID replaces the old entry (useful for updating stale facts).
 */
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    // In-memory index
    private final Map<String, VectorEntry>    index      = new LinkedHashMap<>();
    private final ReadWriteLock               lock       = new ReentrantReadWriteLock();
    private final ExecutorService             writer     =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "vector-writer"));

    private final Path storagePath;
    private final String namespace;

    /**
     * Creates a vector store backed by the given file.
     *
     * @param storagePath path to the .vec NDJSON file
     * @param namespace   human-readable namespace for logging
     */
    public VectorStore(Path storagePath, String namespace) {
        this.storagePath = storagePath;
        this.namespace   = namespace;
        load();
    }

    // ── Write ──────────────────────────────────────────────────────────────

    /**
     * Insert or update an entry. Thread-safe.
     *
     * @param id       unique identifier (stable across updates)
     * @param text     original text (stored for display, not re-embedded)
     * @param vector   embedding vector
     * @param metadata arbitrary key-value metadata
     * @return the stored entry
     */
    public VectorEntry upsert(String id, String text, float[] vector,
                               Map<String, String> metadata) {
        VectorEntry entry = new VectorEntry(id, text, vector,
                metadata != null ? metadata : Map.of(), Instant.now());
        lock.writeLock().lock();
        try {
            index.put(id, entry);
        } finally {
            lock.writeLock().unlock();
        }
        persistAsync();
        log.debug("[vector/{}] upserted id={} dims={}", namespace, id, vector.length);
        return entry;
    }

    /** Delete an entry by ID. Returns true if removed. */
    public boolean delete(String id) {
        lock.writeLock().lock();
        boolean removed;
        try {
            removed = index.remove(id) != null;
        } finally {
            lock.writeLock().unlock();
        }
        if (removed) persistAsync();
        return removed;
    }

    /** Clears all entries in this store. */
    public void clear() {
        lock.writeLock().lock();
        try { index.clear(); }
        finally { lock.writeLock().unlock(); }
        persistAsync();
    }

    // ── Search ─────────────────────────────────────────────────────────────

    /**
     * Find the {@code topK} most similar entries to the query vector.
     *
     * <p>Uses brute-force cosine similarity — O(n·d) where n = number of
     * entries and d = vector dimension. Fast for n &lt; 100 000.
     *
     * @param queryVec   embedding of the search query
     * @param topK       number of results to return
     * @param minScore   minimum cosine similarity threshold (0.0–1.0)
     * @param filterMeta optional metadata filter (all pairs must match)
     * @return ranked list of search results, highest similarity first
     */
    public List<SearchResult> search(float[] queryVec, int topK,
                                      float minScore, Map<String, String> filterMeta) {
        if (queryVec == null || queryVec.length == 0) return List.of();

        lock.readLock().lock();
        List<VectorEntry> snapshot;
        try {
            snapshot = new ArrayList<>(index.values());
        } finally {
            lock.readLock().unlock();
        }

        return snapshot.stream()
                .filter(e -> matchesFilter(e, filterMeta))
                .filter(e -> e.vector() != null && e.vector().length > 0)
                .map(e -> new SearchResult(e, cosineSimilarity(queryVec, e.vector())))
                .filter(r -> r.score() >= minScore)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    /** Convenience: search without metadata filter. */
    public List<SearchResult> search(float[] queryVec, int topK, float minScore) {
        return search(queryVec, topK, minScore, null);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public Optional<VectorEntry> get(String id) {
        lock.readLock().lock();
        try { return Optional.ofNullable(index.get(id)); }
        finally { lock.readLock().unlock(); }
    }

    public int size() {
        lock.readLock().lock();
        try { return index.size(); }
        finally { lock.readLock().unlock(); }
    }

    public List<VectorEntry> all() {
        lock.readLock().lock();
        try { return List.copyOf(index.values()); }
        finally { lock.readLock().unlock(); }
    }

    /** Returns entries with a metadata field matching the given value. */
    public List<VectorEntry> filterByMeta(String key, String value) {
        lock.readLock().lock();
        try {
            return index.values().stream()
                    .filter(e -> value.equals(e.metadata().get(key)))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persistAsync() {
        writer.submit(() -> {
            lock.readLock().lock();
            List<VectorEntry> snapshot;
            try { snapshot = new ArrayList<>(index.values()); }
            finally { lock.readLock().unlock(); }

            try {
                Files.createDirectories(storagePath.getParent());
                // Write to temp file then atomic rename to avoid corrupt reads
                Path tmp = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
                try (var out = Files.newBufferedWriter(tmp)) {
                    for (VectorEntry e : snapshot) {
                        out.write(MAPPER.writeValueAsString(e));
                        out.newLine();
                    }
                }
                Files.move(tmp, storagePath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                log.debug("[vector/{}] persisted {} entries", namespace, snapshot.size());
            } catch (IOException e) {
                log.warn("[vector/{}] persist failed: {}", namespace, e.getMessage());
            }
        });
    }

    private void load() {
        if (!Files.exists(storagePath)) return;
        try (var reader = Files.newBufferedReader(storagePath)) {
            String line;
            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    VectorEntry entry = MAPPER.readValue(line, VectorEntry.class);
                    index.put(entry.id(), entry);
                    loaded++;
                } catch (Exception e) {
                    log.warn("[vector/{}] skipping corrupt entry: {}", namespace, e.getMessage());
                }
            }
            log.info("[vector/{}] loaded {} entries from disk", namespace, loaded);
        } catch (IOException e) {
            log.warn("[vector/{}] load failed: {}", namespace, e.getMessage());
        }
    }

    // ── Math ───────────────────────────────────────────────────────────────

    static float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += (double) a[i] * b[i];
            na  += (double) a[i] * a[i];
            nb  += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom < 1e-9f ? 0f : (float) (dot / denom);
    }

    private boolean matchesFilter(VectorEntry entry, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) return true;
        for (Map.Entry<String, String> kv : filter.entrySet()) {
            if (!kv.getValue().equals(entry.metadata().get(kv.getKey()))) return false;
        }
        return true;
    }

    // ── Types ──────────────────────────────────────────────────────────────

    /**
     * A persisted vector entry.
     *
     * @param id        unique stable identifier
     * @param text      original text (for display and re-ranking)
     * @param vector    embedding coordinates
     * @param metadata  arbitrary key-value tags (type, source, project, etc.)
     * @param createdAt insertion timestamp
     */
    public record VectorEntry(
            String              id,
            String              text,
            float[]             vector,
            Map<String, String> metadata,
            Instant             createdAt
    ) {}

    /**
     * A ranked search result.
     *
     * @param entry the matched vector entry
     * @param score cosine similarity score (0.0–1.0)
     */
    public record SearchResult(VectorEntry entry, float score) {
        public String text()     { return entry.text(); }
        public String id()       { return entry.id(); }
        public String metaGet(String key) {
            return entry.metadata().getOrDefault(key, "");
        }
    }
}
