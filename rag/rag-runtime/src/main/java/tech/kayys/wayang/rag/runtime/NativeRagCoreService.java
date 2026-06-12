package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.util.List;
import java.util.Map;

/**
 * Core service coordinating native RAG ingestion, retrieval, and generation.
 * Pipeline assembly lives in {@link NativeRagPipelineFactory}; this service
 * applies query/result plugin hooks around that pipeline boundary.
 */
@ApplicationScoped
public class NativeRagCoreService {

    @Inject
    NativeGenerationService nativeGenerationService;

    @Inject
    RagPluginManager pluginManager;

    @Inject
    NativeRagPipelineFactory pipelineFactory;

    public List<RagChunk> ingestText(
            String tenantId,
            String source,
            String content,
            Map<String, Object> metadata,
            ChunkingConfig chunkingConfig) {

        return pipelineFactory
                .createRetrievalOnly(tenantId)
                .ingest(source, content, metadata, NativeRagChunkingOptions.from(chunkingConfig));
    }

    public RagResult query(
            String tenantId,
            String query,
            RetrievalConfig retrievalConfig,
            GenerationConfig generationConfig,
            Map<String, Object> filters) {

        NativeRagRetrievalStage retrievalStage = NativeRagRetrievalStage.run(
                tenantId,
                NativeRagQueryContext.create(
                        tenantId,
                        query,
                        retrievalConfig,
                        generationConfig,
                        filters,
                        false),
                pluginManager,
                pipelineFactory);

        String answer = nativeGenerationService.generate(
                retrievalStage.queryContext().ragQuery(),
                retrievalStage.chunks(),
                retrievalStage.queryContext().generationConfig());
        RagResult result = retrievalStage.withAnswer(answer);
        if (pluginManager == null) {
            return result;
        }
        return pluginManager.applyAfterResult(retrievalStage.queryContext().pluginContext(), result);
    }

    public List<RagScoredChunk> retrieve(
            String tenantId,
            String query,
            RetrievalConfig retrievalConfig,
            Map<String, Object> filters) {

        NativeRagRetrievalStage retrievalStage = NativeRagRetrievalStage.run(
                tenantId,
                NativeRagQueryContext.create(
                        tenantId,
                        query,
                        retrievalConfig,
                        GenerationConfig.defaults(),
                        filters,
                        true),
                pluginManager,
                pipelineFactory);
        return retrievalStage.chunks();
    }
}
