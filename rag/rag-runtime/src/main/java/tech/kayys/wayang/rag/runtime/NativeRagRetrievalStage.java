package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Map;

record NativeRagRetrievalStage(
        NativeRagQueryContext queryContext,
        RagResult retrievalResult,
        List<RagScoredChunk> chunks) {

    static NativeRagRetrievalStage run(
            String tenantId,
            NativeRagQueryContext queryContext,
            RagPluginManager pluginManager,
            NativeRagPipelineFactory pipelineFactory) {

        NativeRagQueryContext preparedContext = applyBeforeQuery(queryContext, pluginManager);
        RagResult retrievalResult = pipelineFactory
                .createRetrievalOnly(tenantId)
                .query(preparedContext.ragQuery());
        List<RagScoredChunk> chunks = RagScoredChunks.valid(applyAfterRetrieve(
                preparedContext,
                pluginManager,
                RagScoredChunks.fromResult(retrievalResult)));

        return new NativeRagRetrievalStage(preparedContext, retrievalResult, chunks);
    }

    RagResult withAnswer(String answer) {
        return new RagResult(
                queryContext.ragQuery(),
                chunks,
                answer,
                metadata());
    }

    private Map<String, Object> metadata() {
        return RagRuntimeMetadata.fromResult(retrievalResult);
    }

    private static NativeRagQueryContext applyBeforeQuery(
            NativeRagQueryContext queryContext,
            RagPluginManager pluginManager) {

        if (pluginManager == null) {
            return queryContext;
        }
        return queryContext.withPluginContext(pluginManager.applyBeforeQuery(queryContext.pluginContext()));
    }

    private static List<RagScoredChunk> applyAfterRetrieve(
            NativeRagQueryContext queryContext,
            RagPluginManager pluginManager,
            List<RagScoredChunk> chunks) {

        if (pluginManager == null) {
            return chunks;
        }
        return pluginManager.applyAfterRetrieve(queryContext.pluginContext(), chunks);
    }

}
