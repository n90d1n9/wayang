package tech.kayys.wayang.rag.runtime;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

public record RagQueryRequest(
        String tenantId,
        String query,
        RagMode ragMode,
        SearchStrategy searchStrategy,
        RetrievalConfig retrievalConfig,
        GenerationConfig generationConfig,
        List<String> collections,
        Map<String, Object> filters) {
}