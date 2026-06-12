package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResponseMetadataTest {

    @Test
    void serializesWorkflowMetadata() {
        RagQueryWorkflowContext context = RagQueryWorkflowContext.fromRequest(new RagQueryRequest(
                "tenant",
                "question",
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of("docs"),
                Map.of()));

        Map<String, Object> metadata = RagResponseMetadata.from(context);

        assertEquals(RagMode.STANDARD.name(), metadata.get(RagResponseMetadata.RAG_MODE));
        assertEquals(SearchStrategy.HYBRID.name(), metadata.get(RagResponseMetadata.SEARCH_STRATEGY));
        assertEquals(
                NativeGenerationMode.CONTEXT.name(),
                generationConfig(metadata).get(NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE));
        assertEquals(RetrievalConfig.defaults().topK(), retrievalConfig(metadata).get(RagResponseMetadata.TOP_K));
    }

    @Test
    void serializesNativeGenerationModeFromAdditionalParams() {
        GenerationConfig config = new GenerationConfig(
                "native",
                "model",
                0.2f,
                256,
                1.0f,
                0.0f,
                0.0f,
                List.of(),
                "system",
                Map.of(NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE, "extractive"),
                false,
                false,
                null,
                false,
                false,
                Map.of());

        Map<String, Object> metadata = RagResponseMetadata.generationConfig(config);

        assertEquals("native", metadata.get(RagResponseMetadata.PROVIDER));
        assertEquals("model", metadata.get(RagResponseMetadata.MODEL));
        assertEquals(0.2f, metadata.get(RagResponseMetadata.TEMPERATURE));
        assertEquals(256, metadata.get(RagResponseMetadata.MAX_TOKENS));
        assertEquals("system", metadata.get(RagResponseMetadata.SYSTEM_PROMPT));
        assertEquals(
                NativeGenerationMode.EXTRACTIVE.name(),
                metadata.get(NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE));
    }

    @Test
    void defaultsNullConfigSerializers() {
        assertEquals(
                GenerationConfig.defaults().model(),
                RagResponseMetadata.generationConfig(null).get(RagResponseMetadata.MODEL));
        assertEquals(
                RetrievalConfig.defaults().topK(),
                RagResponseMetadata.retrievalConfig(null).get(RagResponseMetadata.TOP_K));
    }

    @Test
    void preservesNullableGenerationFields() {
        GenerationConfig config = new GenerationConfig(
                null,
                null,
                0.0f,
                0,
                1.0f,
                0.0f,
                0.0f,
                List.of(),
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                null);

        Map<String, Object> metadata = RagResponseMetadata.generationConfig(config);

        assertTrue(metadata.containsKey(RagResponseMetadata.PROVIDER));
        assertTrue(metadata.containsKey(RagResponseMetadata.MODEL));
        assertTrue(metadata.containsKey(RagResponseMetadata.SYSTEM_PROMPT));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> generationConfig(Map<String, Object> metadata) {
        return (Map<String, Object>) metadata.get(RagResponseMetadata.GENERATION_CONFIG);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> retrievalConfig(Map<String, Object> metadata) {
        return (Map<String, Object>) metadata.get(RagResponseMetadata.RETRIEVAL_CONFIG);
    }
}
