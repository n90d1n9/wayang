package tech.kayys.wayang.rag.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.store.VectorSearchHit;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DENSE RETRIEVAL STRATEGY - INTERNAL IMPLEMENTATION
 */
public class DenseRetrievalStrategy implements RetrievalStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DenseRetrievalStrategy.class);
    private final EmbeddingService embeddingService;

    DenseRetrievalStrategy(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public List<ScoredDocument> retrieve(
            String query,
            VectorStore<RagChunk> store,
            RetrievalConfig config) {

        LOG.debug("Dense retrieval for query: {}", query);

        try {
            // Generate query embedding via Wayang Embedding Service
            // For now using default model/provider as configured in the service
            float[] queryVector = embeddingService.embed(EmbeddingRequest.single(query))
                    .await().indefinitely()
                    .first();

            // Search in internal vector store
            // Note: Namespace is often synonymous with tenantId in this architecture
            String namespace = "default";
            Map<String, Object> filters = config.metadataFilters() != null ? new HashMap<>(config.metadataFilters())
                    : new HashMap<>();

            List<VectorSearchHit<RagChunk>> hits = store.search(
                    namespace,
                    queryVector,
                    config.topK(),
                    config.minScore(),
                    filters);

            // Convert to scored documents
            return hits.stream()
                    .map(hit -> new ScoredDocument(hit.payload(), hit.score()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LOG.error("Dense retrieval failed", e);
            return List.of();
        }
    }
}