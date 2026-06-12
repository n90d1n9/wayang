package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.impl.RagPipeline;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;
import tech.kayys.wayang.rag.core.store.InMemoryVectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeRagPipelineFactoryTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RagVectorStoreProvider vectorStoreProvider;

    @Test
    void createsNativePipelineWithSharedIndexerRetrieverAndGenerator() {
        NativeRagPipelineFactory factory = factory();
        when(vectorStoreProvider.getStore()).thenReturn(new InMemoryVectorStore<>());
        when(embeddingService.embedForTenant(eq("tenant-a"), any(EmbeddingRequest.class)))
                .thenAnswer(invocation -> embed(invocation.getArgument(1)));

        RagPipeline pipeline = factory.create(
                "tenant-a",
                (query, context) -> "answer:" + context.size() + ":" + query.text());

        List<RagChunk> chunks = pipeline.ingest(
                "doc-1",
                "alpha document",
                Map.of(RagMetadataKeys.SOURCE, "unit-test"),
                new ChunkingOptions(256, 0));
        RagResult result = pipeline.query(new RagQuery("alpha", 5, 0.0, Map.of()));

        assertEquals(1, chunks.size());
        assertEquals("answer:1:alpha", result.answer());
        assertEquals(1, result.chunks().size());
        assertEquals("doc-1", result.chunks().getFirst().chunk().documentId());
        assertEquals(1, result.metadata().get("retrieved"));
    }

    @Test
    void createsRetrievalOnlyPipelineWithBlankAnswer() {
        NativeRagPipelineFactory factory = factory();
        when(vectorStoreProvider.getStore()).thenReturn(new InMemoryVectorStore<>());
        when(embeddingService.embedForTenant(eq(RagRuntimeDefaults.DEFAULT_NAMESPACE), any(EmbeddingRequest.class)))
                .thenAnswer(invocation -> embed(invocation.getArgument(1)));

        RagPipeline pipeline = factory.createRetrievalOnly(null);
        pipeline.ingest("doc-1", "alpha document", Map.of(), new ChunkingOptions(256, 0));

        RagResult result = pipeline.query(new RagQuery("alpha", 5, 0.0, Map.of()));

        assertEquals("", result.answer());
        assertEquals(1, result.chunks().size());
        assertTrue(result.chunks().getFirst().score() > 0.99);
    }

    private NativeRagPipelineFactory factory() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setEmbeddingModel("test-2");

        NativeRagPipelineFactory factory = new NativeRagPipelineFactory();
        factory.embeddingService = embeddingService;
        factory.config = config;
        factory.vectorStoreProvider = vectorStoreProvider;
        return factory;
    }

    private static Uni<EmbeddingResponse> embed(EmbeddingRequest request) {
        List<float[]> vectors = request.inputs().stream()
                .map(NativeRagPipelineFactoryTest::vectorFor)
                .toList();
        return Uni.createFrom().item(new EmbeddingResponse(
                vectors,
                2,
                "test",
                request.model(),
                "v1"));
    }

    private static float[] vectorFor(String input) {
        String value = input == null ? "" : input.toLowerCase();
        if (value.contains("alpha")) {
            return new float[] { 1.0f, 0.0f };
        }
        return new float[] { 0.0f, 1.0f };
    }
}
