package tech.kayys.wayang.rag.core.store;

import tech.kayys.wayang.vector.faiss.FaissIndex;
import tech.kayys.wayang.vector.faiss.FaissNative;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FAISS implementation of RAG VectorStore using FFM.
 * Supports namespacing by maintaining separate FAISS indexes per namespace.
 */
public class FaissVectorStore<T> implements VectorStore<T>, AutoCloseable {

    private final Map<String, FaissIndex> indexes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Entry<T>>> entryMaps = new ConcurrentHashMap<>();
    private final int dimensions;
    private final String indexDescription;
    private final int metricType;

    public FaissVectorStore(int dimensions, String indexDescription, int metricType) {
        this.dimensions = dimensions;
        this.indexDescription = indexDescription != null ? indexDescription : "Flat";
        this.metricType = metricType;
    }

    public FaissVectorStore(int dimensions) {
        this(dimensions, "Flat", FaissNative.METRIC_L2);
    }

    @Override
    public void upsert(String namespace, String id, float[] vector, T payload, Map<String, Object> metadata) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(vector, "vector must not be null");

        if (vector.length != dimensions) {
            throw new IllegalArgumentException(
                    "Vector dimension mismatch: expected " + dimensions + " but got " + vector.length);
        }

        FaissIndex index = getOrCreateIndex(namespace);
        Map<String, Entry<T>> entryMap = entryMaps.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

        // Add to native FAISS index
        index.add(id, vector);

        // Store payload and metadata
        entryMap.put(id, new Entry<>(id, payload, metadata));
    }

    @Override
    public List<VectorSearchHit<T>> search(
            String namespace,
            float[] queryVector,
            int topK,
            double minScore,
            Map<String, Object> filters) {

        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(queryVector, "queryVector must not be null");

        FaissIndex index = indexes.get(namespace);
        if (index == null) {
            return List.of();
        }

        if (queryVector.length != dimensions) {
            throw new IllegalArgumentException(
                    "Query vector dimension mismatch: expected " + dimensions + " but got " + queryVector.length);
        }

        List<FaissIndex.SearchResult> results = index.search(queryVector, topK);
        Map<String, Entry<T>> entryMap = entryMaps.getOrDefault(namespace, Map.of());

        List<VectorSearchHit<T>> hits = new ArrayList<>();
        for (FaissIndex.SearchResult result : results) {
            if (result.score() < minScore) {
                continue;
            }

            Entry<T> entry = entryMap.get(result.id());
            if (entry != null) {
                // Apply filters if any
                if (matchesFilters(entry.metadata(), filters)) {
                    hits.add(new VectorSearchHit<>(entry.id(), entry.payload(), result.score(), entry.metadata()));
                }
            }
        }

        return hits;
    }

    @Override
    public boolean delete(String namespace, String id) {
        FaissIndex index = indexes.get(namespace);
        if (index == null) {
            return false;
        }

        Map<String, Entry<T>> entryMap = entryMaps.get(namespace);
        if (entryMap != null) {
            entryMap.remove(id);
        }

        index.markForRemoval(id);
        return true;
    }

    @Override
    public void clear(String namespace) {
        FaissIndex index = indexes.remove(namespace);
        if (index != null) {
            index.close();
        }
        entryMaps.remove(namespace);
    }

    private FaissIndex getOrCreateIndex(String namespace) {
        return indexes.computeIfAbsent(namespace, k -> new FaissIndex(dimensions, indexDescription, metricType));
    }

    private boolean matchesFilters(Map<String, Object> metadata, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Object candidate = metadata.get(filter.getKey());
            if (!Objects.equals(candidate, filter.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        for (FaissIndex index : indexes.values()) {
            index.close();
        }
        indexes.clear();
        entryMaps.clear();
    }

    private record Entry<T>(String id, T payload, Map<String, Object> metadata) {
    }
}
