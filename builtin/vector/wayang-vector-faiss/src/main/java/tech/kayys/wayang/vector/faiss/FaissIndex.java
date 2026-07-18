package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FAISS Index backed by native FAISS C library via JDK 25 FFM.
 * <p>
 * Wraps a native {@code FaissIndex} pointer and provides Java-friendly
 * operations for vector storage, search, and persistence.
 * <p>
 * This class manages a mapping between application-level String IDs
 * and FAISS internal {@code idx_t} (long) IDs via an IDMap wrapper.
 */
public class FaissIndex implements AutoCloseable {

    private final Arena arena;
    private MemorySegment indexPtr;
    private final int dimension;
    private final String indexDescription;
    private final int metricType;

    // String ID <-> FAISS idx_t mapping
    private final Map<String, Long> stringToFaissId = new ConcurrentHashMap<>();
    private final Map<Long, String> faissIdToString = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    private volatile boolean closed = false;

    /**
     * Create a new FAISS index.
     *
     * @param dimension        vector dimension
     * @param indexDescription FAISS index factory description (e.g. "Flat", "HNSW32", "IVF100,Flat")
     * @param metricType       metric type (see {@link FaissNative} constants)
     */
    public FaissIndex(int dimension, String indexDescription, int metricType) {
        this.dimension = dimension;
        this.indexDescription = indexDescription;
        this.metricType = metricType;
        this.arena = Arena.ofShared();

        // Create the base index
        MemorySegment baseIndex = FaissIndexBindings.indexFactory(arena, dimension, indexDescription, metricType);
        
        // Wrap the base index in an IDMap so it supports add_with_ids
        this.indexPtr = FaissMetaIndexBindings.newIDMap(arena, baseIndex);
    }

    /**
     * Create a new FAISS index with L2 metric (default).
     */
    public FaissIndex(int dimension, String indexDescription) {
        this(dimension, indexDescription, FaissNative.METRIC_L2);
    }

    /**
     * Create a flat L2 index with the given dimension.
     */
    public FaissIndex(int dimension) {
        this(dimension, "Flat", FaissNative.METRIC_L2);
    }

    // ==================== Training ====================

    /**
     * Train the index on representative vectors.
     * Required for IVF, PQ, and other learned index types.
     *
     * @param trainingVectors training data (n vectors of dimension d)
     */
    public void train(float[][] trainingVectors) {
        checkNotClosed();
        try (Arena local = Arena.ofConfined()) {
            long n = trainingVectors.length;
            MemorySegment vectors = allocateFloatMatrix(local, trainingVectors);
            FaissIndexBindings.train(indexPtr, n, vectors);
        }
    }

    /**
     * Check if the index is trained and ready for adding/searching.
     */
    public boolean isTrained() {
        checkNotClosed();
        return FaissIndexBindings.isTrained(indexPtr);
    }

    // ==================== Adding Vectors ====================

    /**
     * Add a single vector with a string ID.
     */
    public void add(String id, float[] vector) {
        checkNotClosed();
        if (vector.length != dimension) {
            throw new IllegalArgumentException(
                    "Dimension mismatch: expected " + dimension + ", got " + vector.length);
        }

        long faissId = nextId.getAndIncrement();
        stringToFaissId.put(id, faissId);
        faissIdToString.put(faissId, id);

        try (Arena local = Arena.ofConfined()) {
            MemorySegment vectors = local.allocate(ValueLayout.JAVA_FLOAT, vector.length);
            vectors.copyFrom(MemorySegment.ofArray(vector));
            MemorySegment ids = local.allocate(ValueLayout.JAVA_LONG, 1);
            ids.set(ValueLayout.JAVA_LONG, 0, faissId);
            FaissIndexBindings.addWithIds(indexPtr, 1, vectors, ids);
        }
    }

    /**
     * Add multiple vectors with string IDs.
     */
    public void addBatch(String[] ids, float[][] vectors) {
        checkNotClosed();
        if (ids.length != vectors.length) {
            throw new IllegalArgumentException("ids and vectors must have the same length");
        }

        long n = ids.length;
        long[] faissIds = new long[(int) n];
        for (int i = 0; i < n; i++) {
            long faissId = nextId.getAndIncrement();
            stringToFaissId.put(ids[i], faissId);
            faissIdToString.put(faissId, ids[i]);
            faissIds[i] = faissId;
        }

        try (Arena local = Arena.ofConfined()) {
            MemorySegment vectorSeg = allocateFloatMatrix(local, vectors);
            MemorySegment idSeg = local.allocate(ValueLayout.JAVA_LONG, faissIds.length);
            idSeg.copyFrom(MemorySegment.ofArray(faissIds));
            FaissIndexBindings.addWithIds(indexPtr, n, vectorSeg, idSeg);
        }
    }

    // ==================== Searching ====================

    /**
     * Search result containing ID, distance, and optional score.
     */
    public record SearchResult(String id, float distance, float score) {
        public SearchResult(String id, float distance) {
            this(id, distance, 1.0f / (1.0f + distance));
        }
    }

    /**
     * Search for k nearest neighbors.
     *
     * @param queryVector query vector
     * @param k           number of neighbors
     * @return list of search results sorted by similarity
     */
    public List<SearchResult> search(float[] queryVector, int k) {
        checkNotClosed();
        if (queryVector.length != dimension) {
            throw new IllegalArgumentException(
                    "Dimension mismatch: expected " + dimension + ", got " + queryVector.length);
        }

        long nTotal = FaissIndexBindings.getNTotal(indexPtr);
        if (nTotal == 0) {
            return Collections.emptyList();
        }

        // Clamp k to actual index size
        int effectiveK = (int) Math.min(k, nTotal);

        try (Arena local = Arena.ofConfined()) {
            MemorySegment query = local.allocate(ValueLayout.JAVA_FLOAT, queryVector.length);
            query.copyFrom(MemorySegment.ofArray(queryVector));

            MemorySegment distances = local.allocate(ValueLayout.JAVA_FLOAT, effectiveK);
            MemorySegment labels = local.allocate(ValueLayout.JAVA_LONG, effectiveK);

            FaissIndexBindings.search(indexPtr, 1, query, effectiveK, distances, labels);

            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < effectiveK; i++) {
                long label = labels.getAtIndex(ValueLayout.JAVA_LONG, i);
                if (label < 0) continue; // -1 means not found

                String id = faissIdToString.get(label);
                if (id != null) {
                    float dist = distances.getAtIndex(ValueLayout.JAVA_FLOAT, i);
                    results.add(new SearchResult(id, dist));
                }
            }
            return results;
        }
    }

    /**
     * Batch search for k nearest neighbors.
     */
    public List<List<SearchResult>> batchSearch(float[][] queryVectors, int k) {
        checkNotClosed();
        long n = queryVectors.length;
        long nTotal = FaissIndexBindings.getNTotal(indexPtr);
        if (nTotal == 0) {
            List<List<SearchResult>> empty = new ArrayList<>();
            for (int i = 0; i < n; i++) empty.add(Collections.emptyList());
            return empty;
        }

        int effectiveK = (int) Math.min(k, nTotal);

        try (Arena local = Arena.ofConfined()) {
            MemorySegment queries = allocateFloatMatrix(local, queryVectors);
            MemorySegment distances = local.allocate(ValueLayout.JAVA_FLOAT, n * effectiveK);
            MemorySegment labels = local.allocate(ValueLayout.JAVA_LONG, n * effectiveK);

            FaissIndexBindings.search(indexPtr, n, queries, effectiveK, distances, labels);

            List<List<SearchResult>> allResults = new ArrayList<>();
            for (int q = 0; q < n; q++) {
                List<SearchResult> results = new ArrayList<>();
                for (int i = 0; i < effectiveK; i++) {
                    long idx = q * effectiveK + i;
                    long label = labels.getAtIndex(ValueLayout.JAVA_LONG, idx);
                    if (label < 0) continue;

                    String id = faissIdToString.get(label);
                    if (id != null) {
                        float dist = distances.getAtIndex(ValueLayout.JAVA_FLOAT, idx);
                        results.add(new SearchResult(id, dist));
                    }
                }
                allResults.add(results);
            }
            return allResults;
        }
    }

    // ==================== Deletion ====================

    /**
     * Mark a vector for removal (by rebuilding without it).
     */
    public void markForRemoval(String id) {
        Long faissId = stringToFaissId.remove(id);
        if (faissId != null) {
            faissIdToString.remove(faissId);
        }
    }

    /**
     * Reset (clear) the entire index.
     */
    public void reset() {
        checkNotClosed();
        FaissIndexBindings.reset(indexPtr);
        stringToFaissId.clear();
        faissIdToString.clear();
        nextId.set(1);
    }

    // ==================== Reconstruction ====================

    /**
     * Reconstruct a stored vector by its string ID.
     */
    public float[] reconstruct(String id) {
        checkNotClosed();
        Long faissId = stringToFaissId.get(id);
        if (faissId == null) {
            throw new IllegalArgumentException("Unknown vector ID: " + id);
        }

        try (Arena local = Arena.ofConfined()) {
            MemorySegment recons = local.allocate(ValueLayout.JAVA_FLOAT, dimension);
            FaissIndexBindings.reconstruct(indexPtr, faissId, recons);
            float[] result = new float[dimension];
            MemorySegment.ofArray(result).copyFrom(recons);
            return result;
        }
    }

    // ==================== Persistence ====================

    /**
     * Save the index to a file.
     */
    public void save(Path path) throws IOException {
        checkNotClosed();
        java.nio.file.Path parent = path.getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
        try {
            FaissIOBindings.writeIndex(indexPtr, path.toAbsolutePath().toString());
        } catch (FaissException e) {
            throw new IOException("Failed to save FAISS index: " + e.getMessage(), e);
        }
    }

    /**
     * Load an index from a file.
     */
    public void load(Path path) throws IOException {
        checkNotClosed();
        if (!java.nio.file.Files.exists(path)) {
            throw new IOException("Index file not found: " + path);
        }
        try {
            // Free old index
            if (indexPtr != null && !indexPtr.equals(MemorySegment.NULL)) {
                FaissIndexBindings.free(indexPtr);
            }
            indexPtr = FaissIOBindings.readIndex(arena, path.toAbsolutePath().toString(), 0);
            
            // Note: FAISS serializes the full index object. 
            // Since we saved an IndexIDMap, readIndex returns an IndexIDMap pointer natively!
            // No need to wrap it again.
        } catch (FaissException e) {
            throw new IOException("Failed to load FAISS index: " + e.getMessage(), e);
        }
    }

    // ==================== Properties ====================

    /**
     * Get the total number of vectors in the index.
     */
    public long size() {
        checkNotClosed();
        return FaissIndexBindings.getNTotal(indexPtr);
    }

    /**
     * Get the dimension of the index.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get the index description string.
     */
    public String getIndexDescription() {
        return indexDescription;
    }

    /**
     * Get the metric type.
     */
    public int getMetricType() {
        return metricType;
    }

    /**
     * Get the native index pointer (for advanced FFM operations).
     */
    public MemorySegment getNativePointer() {
        checkNotClosed();
        return indexPtr;
    }

    // ==================== AutoCloseable ====================

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (indexPtr != null && !indexPtr.equals(MemorySegment.NULL)) {
                FaissIndexBindings.free(indexPtr);
                indexPtr = MemorySegment.NULL;
            }
            arena.close();
            stringToFaissId.clear();
            faissIdToString.clear();
        }
    }

    // ==================== Internal Helpers ====================

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("FaissIndex has been closed");
        }
    }

    private MemorySegment allocateFloatMatrix(Arena arena, float[][] matrix) {
        long totalFloats = (long) matrix.length * matrix[0].length;
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_FLOAT, totalFloats);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                segment.setAtIndex(ValueLayout.JAVA_FLOAT, (long) i * matrix[i].length + j, matrix[i][j]);
            }
        }
        return segment;
    }
}
