package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.ConversationTurn;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.util.List;
import java.util.Map;

record RagQueryWorkflowContext(
        String tenantId,
        String query,
        RagMode mode,
        SearchStrategy strategy,
        RetrievalConfig retrievalConfig,
        GenerationConfig generationConfig,
        List<String> collections,
        Map<String, Object> filters) {

    RagQueryWorkflowContext {
        tenantId = RagRuntimeText.trimToEmpty(tenantId);
        query = RagRuntimeText.trimToEmpty(query);
        mode = mode == null ? RagMode.STANDARD : mode;
        strategy = strategy == null ? SearchStrategy.HYBRID : strategy;
        retrievalConfig = RagRuntimeConfigs.retrievalOrDefault(retrievalConfig);
        generationConfig = RagRuntimeConfigs.generationOrDefault(generationConfig);
        collections = RagWorkflowFilters.normalizeCollections(collections);
        filters = RagWorkflowFilters.copy(filters);
    }

    static RagQueryWorkflowContext simple(String tenantId, String query, String collectionName) {
        return create(
                tenantId,
                query,
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                collectionName == null ? List.of() : List.of(collectionName),
                Map.of());
    }

    static RagQueryWorkflowContext fromRequest(RagQueryRequest request) {
        if (request == null) {
            return create(null, null, null, null, null, null, null, null);
        }
        return create(
                request.tenantId(),
                request.query(),
                request.ragMode(),
                request.searchStrategy(),
                request.retrievalConfig(),
                request.generationConfig(),
                request.collections(),
                request.filters());
    }

    static RagQueryWorkflowContext conversational(
            String tenantId,
            String query,
            String sessionId,
            List<ConversationTurn> history) {

        RagConversationQuery conversation = RagConversationQuery.from(query, sessionId, history);

        return create(
                tenantId,
                conversation.query(),
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of(),
                conversation.metadata());
    }

    Map<String, Object> nativeFilters() {
        return RagWorkflowFilters.nativeFilters(filters, collections);
    }

    private static RagQueryWorkflowContext create(
            String tenantId,
            String query,
            RagMode mode,
            SearchStrategy strategy,
            RetrievalConfig retrievalConfig,
            GenerationConfig generationConfig,
            List<String> collections,
            Map<String, Object> filters) {

        return new RagQueryWorkflowContext(
                tenantId,
                query,
                mode,
                strategy,
                retrievalConfig,
                generationConfig,
                collections,
                filters);
    }
}
