package tech.kayys.wayang.rag.core.impl;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.embedding.EmbeddingModuleConfig;
import tech.kayys.wayang.embedding.EmbeddingProviderRegistry;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.embedding.provider.CharNgramEmbeddingProvider;
import tech.kayys.wayang.embedding.provider.DeterministicHashEmbeddingProvider;
import tech.kayys.wayang.embedding.provider.TfIdfHashEmbeddingProvider;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;
import tech.kayys.wayang.rag.core.store.InMemoryVectorStore;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPipelineTest {

        @Test
        void shouldIngestAndRetrieveUsingOwnComponents() {
                EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
                                new DeterministicHashEmbeddingProvider(),
                                new TfIdfHashEmbeddingProvider(),
                                new CharNgramEmbeddingProvider()));
                EmbeddingModuleConfig config = new EmbeddingModuleConfig();
                config.setDefaultModel("tfidf-512");
                EmbeddingService embeddingService = new EmbeddingService(registry, config);

                VectorStore<tech.kayys.wayang.rag.core.RagChunk> store = new InMemoryVectorStore<>();
                RagIndexer indexer = new RagIndexer(embeddingService, store, "tenant-a", "tfidf-512");
                VectorRetriever retriever = new VectorRetriever(embeddingService, store, "tenant-a", "tfidf-512");

                RagPipeline pipeline = new RagPipeline(
                                new SimpleTextDocumentParser(),
                                new SlidingWindowChunker(),
                                indexer,
                                retriever,
                                new TopKReranker(),
                                new ContextConcatenatingGenerator());

                pipeline.ingest(
                                "runbook",
                                "Payment timeout happens when gateway latency spikes. Retry policy should use exponential backoff.",
                                Map.of("collection", "ops"),
                                new ChunkingOptions(70, 10));

                RagResult result = pipeline.query(new RagQuery(
                                "How to handle payment timeout?",
                                3,
                                0.0,
                                Map.of()));

                assertFalse(result.chunks().isEmpty());
                assertTrue(result.answer().contains("payment") || result.answer().contains("Payment"));
        }
}
