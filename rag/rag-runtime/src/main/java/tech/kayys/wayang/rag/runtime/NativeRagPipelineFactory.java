package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.impl.RagPipeline;
import tech.kayys.wayang.rag.core.spi.Generator;

/**
 * Factory for native RAG pipeline assembly.
 */
@ApplicationScoped
public class NativeRagPipelineFactory {

    @Inject
    EmbeddingService embeddingService;

    @Inject
    RagRuntimeConfig config;

    @Inject
    RagVectorStoreProvider vectorStoreProvider;

    public RagPipeline create(String tenantId, Generator generator) {
        return new NativeRagPipelineSpec(
                tenantId,
                config == null ? null : config.getEmbeddingModel(),
                vectorStoreProvider.getStore(),
                generator)
                .createPipeline(embeddingService);
    }

    public RagPipeline createRetrievalOnly(String tenantId) {
        return create(tenantId, null);
    }
}
