package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.impl.RagIndexer;
import tech.kayys.wayang.rag.core.impl.RagPipeline;
import tech.kayys.wayang.rag.core.impl.SimpleTextDocumentParser;
import tech.kayys.wayang.rag.core.impl.SlidingWindowChunker;
import tech.kayys.wayang.rag.core.impl.TopKReranker;
import tech.kayys.wayang.rag.core.impl.VectorRetriever;
import tech.kayys.wayang.rag.core.spi.Generator;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.Objects;

record NativeRagPipelineSpec(
        String namespace,
        String embeddingModel,
        VectorStore<RagChunk> vectorStore,
        Generator generator) {

    static final String DEFAULT_NAMESPACE = RagRuntimeDefaults.DEFAULT_NAMESPACE;

    NativeRagPipelineSpec {
        namespace = normalizeNamespace(namespace);
        embeddingModel = normalizeEmbeddingModel(embeddingModel);
        vectorStore = Objects.requireNonNull(vectorStore, "vectorStore");
        generator = generator == null ? blankGenerator() : generator;
    }

    RagPipeline createPipeline(EmbeddingService embeddingService) {
        Objects.requireNonNull(embeddingService, "embeddingService");
        return new RagPipeline(
                new SimpleTextDocumentParser(),
                new SlidingWindowChunker(),
                createIndexer(embeddingService),
                createRetriever(embeddingService),
                new TopKReranker(),
                generator);
    }

    private RagIndexer createIndexer(EmbeddingService embeddingService) {
        return new RagIndexer(
                embeddingService,
                vectorStore,
                namespace,
                embeddingModel);
    }

    private VectorRetriever createRetriever(EmbeddingService embeddingService) {
        return new VectorRetriever(
                embeddingService,
                vectorStore,
                namespace,
                embeddingModel);
    }

    private static String normalizeNamespace(String namespace) {
        return RagRuntimeDefaults.normalizeNamespace(namespace);
    }

    private static String normalizeEmbeddingModel(String embeddingModel) {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            return RagRuntimeConfig.DEFAULT_EMBEDDING_MODEL;
        }
        return embeddingModel.trim();
    }

    private static Generator blankGenerator() {
        return (query, context) -> "";
    }
}
