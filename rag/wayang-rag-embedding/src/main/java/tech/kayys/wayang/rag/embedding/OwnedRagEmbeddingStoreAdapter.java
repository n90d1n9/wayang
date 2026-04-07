package tech.kayys.wayang.rag.embedding;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.store.VectorSearchHit;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class OwnedRagEmbeddingStoreAdapter implements RagEmbeddingStore {

    private final String namespace;
    private final String embeddingModel;
    private final int embeddingDimension;
    private final String embeddingVersion;
    private final VectorStore<RagChunk> vectorStore;
    private final EmbeddingMetrics metrics;
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    OwnedRagEmbeddingStoreAdapter(
            String namespace,
            String embeddingModel,
            int embeddingDimension,
            VectorStore<RagChunk> vectorStore) {
        this(namespace, embeddingModel, embeddingDimension, "v1", vectorStore, EmbeddingMetrics.NOOP);
    }

    OwnedRagEmbeddingStoreAdapter(
            String namespace,
            String embeddingModel,
            int embeddingDimension,
            String embeddingVersion,
            VectorStore<RagChunk> vectorStore) {
        this(namespace, embeddingModel, embeddingDimension, embeddingVersion, vectorStore, EmbeddingMetrics.NOOP);
    }

    OwnedRagEmbeddingStoreAdapter(
            String namespace,
            String embeddingModel,
            int embeddingDimension,
            String embeddingVersion,
            VectorStore<RagChunk> vectorStore,
            EmbeddingMetrics metrics) {
        this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        if (embeddingDimension <= 0) {
            throw new IllegalArgumentException("embeddingDimension must be > 0");
        }
        this.embeddingDimension = embeddingDimension;
        this.embeddingVersion = (embeddingVersion == null || embeddingVersion.isBlank()) ? "v1" : embeddingVersion;
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.metrics = metrics != null ? metrics : EmbeddingMetrics.NOOP;
    }

    @Override
    public String add(float[] embedding, String text, Map<String, Object> metadata) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, text, metadata);
        return id;
    }

    @Override
    public void add(String id, float[] embedding, String text, Map<String, Object> metadata) {
        long started = System.currentTimeMillis();
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(embedding, "embedding must not be null");
        if (embedding.length != embeddingDimension) {
            throw new IllegalArgumentException(
                    "Embedding dimension mismatch for namespace '" + namespace
                            + "': expected " + embeddingDimension + " but got " + embedding.length);
        }

        Map<String, Object> safeMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        validateMetadataContract(safeMetadata);
        Map<String, Object> enrichedMetadata = enrichMetadata(safeMetadata);
        RagChunk chunk = toRagChunk(id, text == null ? "" : text, enrichedMetadata);
        vectorStore.upsert(namespace, id, embedding, chunk, enrichedMetadata);
        embeddingCache.put(id, embedding);
        metrics.recordIngestion(namespace, 1, 1, System.currentTimeMillis() - started);
    }

    @Override
    public List<RagEmbeddingMatch> search(
            float[] queryEmbedding,
            int topK,
            double minScore,
            Map<String, Object> filters) {

        long started = System.currentTimeMillis();
        Objects.requireNonNull(queryEmbedding, "queryEmbedding must not be null");
        if (queryEmbedding.length != embeddingDimension) {
            throw new IllegalArgumentException(
                    "Query embedding dimension mismatch for namespace '" + namespace
                            + "': expected " + embeddingDimension + " but got " + queryEmbedding.length);
        }
        try {
            Map<String, Object> safeFilters = filters == null ? Map.of() : Map.copyOf(filters);
            validateFiltersContract(safeFilters);
            Map<String, Object> strictFilters = new java.util.HashMap<>(safeFilters);
            strictFilters.put("tenantId", namespace);
            strictFilters.put("embeddingModel", embeddingModel);
            strictFilters.put("embeddingDimension", embeddingDimension);
            strictFilters.put("embeddingVersion", embeddingVersion);
            List<VectorSearchHit<RagChunk>> hits = vectorStore.search(namespace, queryEmbedding, topK, minScore,
                    strictFilters);
            metrics.recordSearchSuccess(namespace, System.currentTimeMillis() - started, hits.size());
            return hits.stream().map(this::toMatch).toList();
        } catch (RuntimeException ex) {
            metrics.recordSearchFailure(namespace);
            throw ex;
        }
    }

    @Override
    public void remove(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        vectorStore.delete(namespace, id);
        embeddingCache.remove(id);
    }

    @Override
    public void clear() {
        vectorStore.clear(namespace);
        embeddingCache.clear();
    }

    private RagEmbeddingMatch toMatch(VectorSearchHit<RagChunk> hit) {
        RagChunk chunk = hit.payload();
        return new RagEmbeddingMatch(
                hit.score(),
                hit.id(),
                embeddingCache.getOrDefault(hit.id(), new float[0]),
                chunk.text(),
                chunk.metadata());
    }

    private RagChunk toRagChunk(String id, String text, Map<String, Object> metadata) {
        String documentId = String
                .valueOf(metadata.getOrDefault("documentId", metadata.getOrDefault("source", namespace)));
        int chunkIndex = parseInt(metadata.get("chunkIndex"), 0);
        return new RagChunk(id, documentId, chunkIndex, text, metadata);
    }

    private Map<String, Object> enrichMetadata(Map<String, Object> metadata) {
        Map<String, Object> enriched = new java.util.HashMap<>(metadata);
        enriched.put("tenantId", namespace);
        enriched.put("embeddingModel", embeddingModel);
        enriched.put("embeddingDimension", embeddingDimension);
        enriched.put("embeddingVersion", embeddingVersion);
        return Map.copyOf(enriched);
    }

    private void validateMetadataContract(Map<String, Object> metadata) {
        Object tenant = metadata.get("tenantId");
        if (tenant != null && !Objects.equals(namespace, tenant)) {
            throw new IllegalArgumentException(
                    "Metadata tenantId mismatch: expected '" + namespace + "' but got '" + tenant + "'");
        }
        Object model = metadata.get("embeddingModel");
        if (model != null && !Objects.equals(embeddingModel, String.valueOf(model))) {
            throw new IllegalArgumentException(
                    "Metadata embeddingModel mismatch: expected '" + embeddingModel + "' but got '" + model + "'");
        }
        Object dimension = metadata.get("embeddingDimension");
        if (dimension != null && parseInt(dimension, -1) != embeddingDimension) {
            throw new IllegalArgumentException(
                    "Metadata embeddingDimension mismatch: expected " + embeddingDimension + " but got " + dimension);
        }
        Object version = metadata.get("embeddingVersion");
        if (version != null && !Objects.equals(embeddingVersion, String.valueOf(version))) {
            throw new IllegalArgumentException(
                    "Metadata embeddingVersion mismatch: expected '" + embeddingVersion + "' but got '" + version
                            + "'");
        }
    }

    private void validateFiltersContract(Map<String, Object> filters) {
        Object tenant = filters.get("tenantId");
        if (tenant != null && !Objects.equals(namespace, tenant)) {
            throw new IllegalArgumentException(
                    "Filter tenantId mismatch: expected '" + namespace + "' but got '" + tenant + "'");
        }
        Object model = filters.get("embeddingModel");
        if (model != null && !Objects.equals(embeddingModel, String.valueOf(model))) {
            throw new IllegalArgumentException(
                    "Filter embeddingModel mismatch: expected '" + embeddingModel + "' but got '" + model + "'");
        }
        Object dimension = filters.get("embeddingDimension");
        if (dimension != null && parseInt(dimension, -1) != embeddingDimension) {
            throw new IllegalArgumentException(
                    "Filter embeddingDimension mismatch: expected " + embeddingDimension + " but got " + dimension);
        }
        Object version = filters.get("embeddingVersion");
        if (version != null && !Objects.equals(embeddingVersion, String.valueOf(version))) {
            throw new IllegalArgumentException(
                    "Filter embeddingVersion mismatch: expected '" + embeddingVersion + "' but got '" + version + "'");
        }
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
