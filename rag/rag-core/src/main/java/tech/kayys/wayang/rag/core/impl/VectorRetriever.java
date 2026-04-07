package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingModelSpec;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;
import tech.kayys.wayang.rag.core.store.VectorSearchHit;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class VectorRetriever implements Retriever {

    private final EmbeddingService embeddingService;
    private final VectorStore<RagChunk> vectorStore;
    private final String namespace;
    private final String embeddingModel;

    public VectorRetriever(
            EmbeddingService embeddingService,
            VectorStore<RagChunk> vectorStore,
            String namespace,
            String embeddingModel) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.namespace = namespace;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<RagScoredChunk> retrieve(RagQuery query) {
        EmbeddingResponse response = embeddingService.embedForTenant(
                namespace,
                new EmbeddingRequest(List.of(query.text()), embeddingModel, null, true))
                .await().indefinitely();
        validateEmbeddingDimension(response.dimension());
        float[] queryVector = response.first();
        if (queryVector.length != response.dimension()) {
            throw new EmbeddingException(
                    "Query embedding vector size mismatch: expected "
                            + response.dimension() + " but got " + queryVector.length);
        }

        Map<String, Object> strictFilters = new HashMap<>();
        if (query.filters() != null) {
            strictFilters.putAll(query.filters());
        }
        strictFilters.put("tenantId", namespace);
        strictFilters.put("embeddingModel", embeddingModel);
        strictFilters.put("embeddingDimension", response.dimension());
        strictFilters.put("embeddingVersion", response.version());

        List<VectorSearchHit<RagChunk>> hits = vectorStore.search(
                namespace,
                queryVector,
                query.topK(),
                query.minScore(),
                strictFilters);

        return hits.stream()
                .map(hit -> new RagScoredChunk(hit.payload(), hit.score()))
                .toList();
    }

    private void validateEmbeddingDimension(int observedDimension) {
        OptionalInt expected = EmbeddingModelSpec.parseDimension(embeddingModel);
        if (expected.isPresent() && expected.getAsInt() != observedDimension) {
            throw new EmbeddingException(
                    "Query embedding dimension mismatch for namespace '" + namespace + "': model="
                            + embeddingModel + ", expected=" + expected.getAsInt()
                            + ", observed=" + observedDimension);
        }
    }
}
