package tech.kayys.wayang.vector.faiss;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.vector.AbstractVectorStore;
import tech.kayys.wayang.vector.VectorEntry;
import tech.kayys.wayang.vector.VectorQuery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FAISS implementation of VectorStore using JDK 25 FFM.
 * <p>
 * Backed by the native FAISS C library via {@link FaissIndex}.
 * Supports all index types through the FAISS index factory description string
 * (e.g., "Flat", "HNSW32", "IVF100,Flat", "PQ16", "SQ8").
 */
public class FaissVectorStore extends AbstractVectorStore implements AutoCloseable {

    private final FaissIndex index;
    private final Map<String, VectorEntry> entryMap;
    private final String indexFilePath;
    private final int dimension;

    /**
     * Create a new FAISS vector store.
     *
     * @param dimension        the dimension of vectors
     * @param indexDescription FAISS index factory string (e.g. "Flat", "HNSW32", "IVF100,Flat")
     * @param metricType       metric type constant from {@link FaissNative}
     * @param indexFilePath    optional path to persist the index
     */
    public FaissVectorStore(int dimension, String indexDescription, int metricType, String indexFilePath) {
        this.dimension = dimension;
        this.index = new FaissIndex(dimension, indexDescription, metricType);
        this.entryMap = new ConcurrentHashMap<>();
        this.indexFilePath = indexFilePath;

        // Load existing index if file exists
        if (indexFilePath != null && !indexFilePath.isEmpty()) {
            Path path = Path.of(indexFilePath);
            if (java.nio.file.Files.exists(path)) {
                loadIndex(path);
            }
        }
    }

    /**
     * Create a new FAISS vector store with L2 metric.
     */
    public FaissVectorStore(int dimension, String indexDescription, String indexFilePath) {
        this(dimension, indexDescription, FaissNative.METRIC_L2, indexFilePath);
    }

    /**
     * Create a new FAISS vector store with defaults (768 dims, Flat index, L2 metric).
     */
    public FaissVectorStore() {
        this(768, "Flat", FaissNative.METRIC_L2, null);
    }

    /**
     * Create with specific dimension and index type.
     */
    public FaissVectorStore(int dimension, String indexDescription) {
        this(dimension, indexDescription, FaissNative.METRIC_L2, null);
    }

    @Override
    public Uni<Void> store(List<VectorEntry> entries) {
        return Uni.createFrom().item(() -> {
            for (VectorEntry entry : entries) {
                // Validate dimension
                if (entry.vector().size() != dimension) {
                    throw new IllegalArgumentException(
                            "Vector dimension mismatch. Expected " + dimension +
                                    ", got " + entry.vector().size());
                }

                // Convert List<Float> to float[]
                float[] vector = toFloatArray(entry.vector());

                // Add to native FAISS index
                index.add(entry.id(), vector);

                // Store entry in map for metadata retrieval
                entryMap.put(entry.id(), entry);
            }
            return null;
        }).replaceWithVoid()
                .onItem().transformToUni(unused -> {
                    if (indexFilePath != null && !indexFilePath.isEmpty()) {
                        return persistIndex();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<List<VectorEntry>> search(VectorQuery query) {
        return Uni.createFrom().item(() -> {
            if (query.vector().isEmpty()) {
                return Collections.<VectorEntry>emptyList();
            }

            float[] queryVector = toFloatArray(query.vector());

            List<FaissIndex.SearchResult> searchResults = index.search(queryVector, query.topK());

            // Map back to VectorEntry with score filtering
            List<VectorEntry> results = new ArrayList<>();
            for (FaissIndex.SearchResult result : searchResults) {
                if (result.score() < query.minScore()) {
                    continue;
                }
                VectorEntry entry = entryMap.get(result.id());
                if (entry != null) {
                    results.add(entry);
                }
            }

            return results;
        });
    }

    @Override
    public Uni<Void> delete(List<String> ids) {
        return Uni.createFrom().item(() -> {
            for (String id : ids) {
                entryMap.remove(id);
                index.markForRemoval(id);
            }
            return null;
        }).replaceWithVoid();
    }

    // ==================== Index Management ====================

    /**
     * Train the index on representative vectors.
     * Required for IVF, PQ, and other learned index types.
     */
    public void trainIndex(List<List<Float>> trainingVectors) {
        float[][] vectors = trainingVectors.stream()
                .map(this::toFloatArray)
                .toArray(float[][]::new);
        index.train(vectors);
    }

    /**
     * Save the index to disk.
     */
    public void saveIndex(Path path) throws IOException {
        index.save(path);
    }

    /**
     * Load the index from disk.
     */
    public void loadIndex(Path path) {
        try {
            index.load(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FAISS index", e);
        }
    }

    /**
     * Rebuild the index from current entries.
     */
    public Uni<Void> rebuildIndex() {
        return Uni.createFrom().item(() -> {
            index.reset();
            for (VectorEntry entry : entryMap.values()) {
                float[] vector = toFloatArray(entry.vector());
                index.add(entry.id(), vector);
            }
            return null;
        }).replaceWithVoid();
    }

    /**
     * Get the number of vectors in the index.
     */
    public long size() {
        return index.size();
    }

    /**
     * Get the dimension of vectors.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Check if the index is trained.
     */
    public boolean isTrained() {
        return index.isTrained();
    }

    /**
     * Get the native FaissIndex for advanced operations.
     */
    public FaissIndex getIndex() {
        return index;
    }

    @Override
    public void close() {
        index.close();
        entryMap.clear();
    }

    // ==================== Private Helpers ====================

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private Uni<Void> persistIndex() {
        return Uni.createFrom().completionStage(() -> {
            try {
                saveIndex(Path.of(indexFilePath));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            } catch (IOException e) {
                return java.util.concurrent.CompletableFuture.<Void>failedFuture(e);
            }
        });
    }
}
