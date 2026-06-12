package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RagWorkflowInput;
import tech.kayys.wayang.rag.core.RerankingModel;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagWorkflowRequestMapperTest {

    @Test
    void mapsWorkflowInputToAdvancedQueryRequest() {
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        GenerationConfig generationConfig = GenerationConfig.defaults();

        RagQueryRequest request = RagWorkflowRequestMapper.toQueryRequest(
                new RagWorkflowInput(" tenant ", " question ", retrievalConfig, generationConfig));

        assertEquals("tenant", request.tenantId());
        assertEquals("question", request.query());
        assertEquals(RagMode.STANDARD, request.ragMode());
        assertEquals(SearchStrategy.HYBRID, request.searchStrategy());
        assertEquals(retrievalConfig, request.retrievalConfig());
        assertEquals(generationConfig, request.generationConfig());
        assertEquals(List.of(RagWorkflowRequestMapper.DEFAULT_COLLECTION), request.collections());
        assertEquals(Map.of(), request.filters());
    }

    @Test
    void defaultsNullInputConfiguration() {
        RagQueryRequest request = RagWorkflowRequestMapper.toQueryRequest(
                new RagWorkflowInput("tenant", "question", null, null));

        assertEquals(RetrievalConfig.defaults().topK(), request.retrievalConfig().topK());
        assertEquals(GenerationConfig.defaults().model(), request.generationConfig().model());
        assertEquals(List.of(RagWorkflowRequestMapper.DEFAULT_COLLECTION), request.collections());
        assertEquals(Map.of(), request.filters());
    }

    @Test
    void defaultsNullWorkflowInput() {
        RagQueryRequest request = RagWorkflowRequestMapper.toQueryRequest(null);

        assertEquals("", request.tenantId());
        assertEquals("", request.query());
        assertEquals(RetrievalConfig.defaults().topK(), request.retrievalConfig().topK());
        assertEquals(GenerationConfig.defaults().model(), request.generationConfig().model());
        assertEquals(List.of(RagWorkflowRequestMapper.DEFAULT_COLLECTION), request.collections());
        assertEquals(Map.of(), request.filters());
    }

    @Test
    void copiesRetrievalFiltersIntoRequestDefensively() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "docs");
        filters.put("nullable", null);
        RetrievalConfig retrievalConfig = new RetrievalConfig(
                8,
                0.7f,
                1024,
                128,
                true,
                RerankingModel.COHERE_RERANK,
                true,
                0.4f,
                false,
                0,
                false,
                0,
                filters,
                List.of(),
                false,
                true);

        RagQueryRequest request = RagWorkflowRequestMapper.toQueryRequest(
                new RagWorkflowInput("tenant", "question", retrievalConfig, GenerationConfig.defaults()));
        filters.put("domain", "mutated");

        assertEquals("docs", request.filters().get("domain"));
        assertEquals(true, request.filters().containsKey("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> request.filters().put("other", "value"));
    }
}
