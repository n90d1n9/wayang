package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.spi.Generator;
import tech.kayys.wayang.rag.core.store.InMemoryVectorStore;
import tech.kayys.wayang.rag.core.store.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeRagPipelineSpecTest {

    @Test
    void defaultsBlankNamespaceModelAndGenerator() {
        VectorStore<RagChunk> store = new InMemoryVectorStore<>();

        NativeRagPipelineSpec spec = new NativeRagPipelineSpec(" ", " ", store, null);

        assertEquals(NativeRagPipelineSpec.DEFAULT_NAMESPACE, spec.namespace());
        assertEquals(RagRuntimeConfig.DEFAULT_EMBEDDING_MODEL, spec.embeddingModel());
        assertSame(store, spec.vectorStore());
        assertEquals("", spec.generator().generate(new RagQuery("query", 1, 0.0, Map.of()), List.of()));
    }

    @Test
    void trimsConfiguredNamespaceAndEmbeddingModel() {
        VectorStore<RagChunk> store = new InMemoryVectorStore<>();
        Generator generator = (query, context) -> query.text();

        NativeRagPipelineSpec spec = new NativeRagPipelineSpec(" tenant-a ", " model-a ", store, generator);

        assertEquals("tenant-a", spec.namespace());
        assertEquals("model-a", spec.embeddingModel());
        assertSame(store, spec.vectorStore());
        assertSame(generator, spec.generator());
    }

    @Test
    void rejectsMissingRequiredPipelineDependencies() {
        assertThrows(
                NullPointerException.class,
                () -> new NativeRagPipelineSpec("tenant-a", "model-a", null, null));

        NativeRagPipelineSpec spec = new NativeRagPipelineSpec(
                "tenant-a",
                "model-a",
                new InMemoryVectorStore<>(),
                null);
        assertThrows(NullPointerException.class, () -> spec.createPipeline(null));
    }
}
