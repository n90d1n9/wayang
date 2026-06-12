package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.ConversationTurn;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

/**
 * High-level service for executing RAG queries via the native RAG engine.
 * Supports simple queries, advanced queries with full configuration,
 * and conversational (multi-turn) RAG by enhancing queries with conversation
 * history.
 */
@ApplicationScoped
public class RagQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(RagQueryService.class);

    @Inject
    NativeRagCoreService nativeRagCoreService;

    @Inject
    RagObservabilityMetrics metricsRecorder = new RagObservabilityMetrics();

    /**
     * Execute simple RAG query
     */
    public Uni<RagResponse> query(
            String tenantId,
            String query,
            String collectionName) {

        return executeRagWorkflow(RagQueryWorkflowContext.simple(tenantId, query, collectionName));
    }

    /**
     * Execute advanced RAG query with full configuration
     */
    public Uni<RagResponse> advancedQuery(RagQueryRequest request) {

        return executeRagWorkflow(RagQueryWorkflowContext.fromRequest(request));
    }

    /**
     * Execute multi-turn conversational RAG
     */
    public Uni<RagResponse> conversationalQuery(
            String tenantId,
            String query,
            String sessionId,
            List<ConversationTurn> history) {

        return executeRagWorkflow(RagQueryWorkflowContext.conversational(tenantId, query, sessionId, history));
    }

    private Uni<RagResponse> executeRagWorkflow(RagQueryWorkflowContext context) {

        LOG.info("Executing RAG workflow for tenant: {}, query: {}", context.tenantId(), context.query());

        return Uni.createFrom().item(() -> {
            long started = System.currentTimeMillis();

            RagResult result = nativeRagCoreService.query(
                    context.tenantId(),
                    context.query(),
                    context.retrievalConfig(),
                    context.generationConfig(),
                    context.nativeFilters());
            List<RagScoredChunk> chunks = RagScoredChunks.fromResult(result);

            metricsRecorder.recordSearchSuccess(
                    context.tenantId(),
                    System.currentTimeMillis() - started,
                    chunks.size());

            return RagQueryResponseAssembler.toResponse(context, result, chunks);
        }).onFailure().invoke(ignored -> metricsRecorder.recordSearchFailure(context.tenantId()));
    }
}
