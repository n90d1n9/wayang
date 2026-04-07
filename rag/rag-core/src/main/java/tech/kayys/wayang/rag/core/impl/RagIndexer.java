package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingModelSpec;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagDocument;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class RagIndexer {

    private final EmbeddingService embeddingService;
    private final VectorStore<RagChunk> vectorStore;
    private final String namespace;
    private final String embeddingModel;

    public RagIndexer(
            EmbeddingService embeddingService,
            VectorStore<RagChunk> vectorStore,
            String namespace,
            String embeddingModel) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.namespace = namespace;
        this.embeddingModel = embeddingModel;
    }

    public void indexChunks(List<RagChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<String> inputs = chunks.stream().map(RagChunk::text).toList();
        EmbeddingResponse response = embeddingService.embedForTenant(
                namespace,
                new EmbeddingRequest(inputs, embeddingModel, null, true))
                .await().indefinitely();
        validateEmbeddingDimension(response.dimension());

        List<float[]> vectors = response.embeddings();
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            float[] vector = vectors.get(i);
            if (vector.length != response.dimension()) {
                throw new EmbeddingException(
                        "Embedding vector size mismatch at index " + i + ": expected "
                                + response.dimension() + " but got " + vector.length);
            }
            vectorStore.upsert(
                    namespace,
                    chunk.id(),
                    vector,
                    chunk,
                    Map.of(
                            "tenantId", namespace,
                            "embeddingModel", embeddingModel,
                            "embeddingDimension", response.dimension(),
                            "embeddingVersion", response.version(),
                            "documentId", chunk.documentId(),
                            "chunkIndex", chunk.chunkIndex()));
        }
    }

    private void validateEmbeddingDimension(int observedDimension) {
        OptionalInt expected = EmbeddingModelSpec.parseDimension(embeddingModel);
        if (expected.isPresent() && expected.getAsInt() != observedDimension) {
            throw new EmbeddingException(
                    "Embedding dimension mismatch for namespace '" + namespace + "': model="
                            + embeddingModel + ", expected=" + expected.getAsInt()
                            + ", observed=" + observedDimension);
        }
    }
}
