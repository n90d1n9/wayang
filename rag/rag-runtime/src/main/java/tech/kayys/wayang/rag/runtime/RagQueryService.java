package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.*;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.time.Instant;
import java.util.*;

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

        return executeRagWorkflow(
                tenantId,
                query,
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of(collectionName),
                Map.of());
    }

    /**
     * Execute advanced RAG query with full configuration
     */
    public Uni<RagResponse> advancedQuery(RagQueryRequest request) {

        return executeRagWorkflow(
                request.tenantId(),
                request.query(),
                request.ragMode(),
                request.searchStrategy(),
                request.retrievalConfig(),
                request.generationConfig(),
                request.collections(),
                request.filters());
    }

    /**
     * Execute multi-turn conversational RAG
     */
    public Uni<RagResponse> conversationalQuery(
            String tenantId,
            String query,
            String sessionId,
            List<ConversationTurn> history) {

        // Build context from history
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("conversationHistory", history);

        // Enhance query with conversation context
        String enhancedQuery = enhanceQueryWithHistory(query, history);

        return executeRagWorkflow(
                tenantId,
                enhancedQuery,
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of(),
                metadata);
    }

    private Uni<RagResponse> executeRagWorkflow(
            String tenantId,
            String query,
            RagMode mode,
            SearchStrategy strategy,
            RetrievalConfig retrievalConfig,
            GenerationConfig generationConfig,
            List<String> collections,
            Map<String, Object> filters) {

        LOG.info("Executing RAG workflow for tenant: {}, query: {}", tenantId, query);

        return Uni.createFrom().item(() -> {
            long started = System.currentTimeMillis();
            Map<String, Object> nativeFilters = new HashMap<>();
            if (filters != null) {
                nativeFilters.putAll(filters);
            }
            if (collections != null && !collections.isEmpty()) {
                nativeFilters.put("collection", collections.get(0));
            }

            RagResult result = nativeRagCoreService.query(
                    tenantId,
                    query,
                    retrievalConfig,
                    generationConfig,
                    nativeFilters);

            metricsRecorder.recordSearchSuccess(
                    tenantId,
                    System.currentTimeMillis() - started,
                    result.chunks() == null ? 0 : result.chunks().size());

            List<SourceDocument> sourceDocuments = result.chunks().stream()
                    .map(this::toSourceDocument)
                    .toList();

            RagMetrics metrics = new RagMetrics(
                    0L,
                    sourceDocuments.size(),
                    0,
                    averageScore(result.chunks()),
                    result.chunks().size(),
                    0,
                    true);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ragMode", mode.name());
            metadata.put("searchStrategy", strategy.name());
            metadata.put("generationConfig", serializeGenerationConfig(generationConfig));
            metadata.put("retrievalConfig", serializeRetrievalConfig(retrievalConfig));

            return new RagResponse(
                    query,
                    result.answer(),
                    sourceDocuments,
                    List.of(),
                    metrics,
                    result.answer(),
                    Instant.now(),
                    metadata,
                    collections == null ? List.of() : collections,
                    Optional.empty());
        }).onFailure().invoke(ignored -> metricsRecorder.recordSearchFailure(tenantId));
    }

    private String enhanceQueryWithHistory(
            String query,
            List<ConversationTurn> history) {

        if (history.isEmpty()) {
            return query;
        }

        StringBuilder enhanced = new StringBuilder();
        enhanced.append("Previous conversation:\n");

        for (ConversationTurn turn : history) {
            enhanced.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }

        enhanced.append("\nCurrent question: ").append(query);

        return enhanced.toString();
    }

    private Map<String, Object> serializeRetrievalConfig(RetrievalConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("topK", config.topK());
        map.put("minSimilarity", config.minSimilarity());
        map.put("maxChunkSize", config.maxChunkSize());
        map.put("chunkOverlap", config.chunkOverlap());
        map.put("enableReranking", config.enableReranking());
        map.put("enableHybridSearch", config.enableHybridSearch());
        map.put("hybridAlpha", config.hybridAlpha());
        return map;
    }

    private Map<String, Object> serializeGenerationConfig(GenerationConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", config.provider());
        map.put("model", config.model());
        map.put("temperature", config.temperature());
        map.put("maxTokens", config.maxTokens());
        map.put("systemPrompt", config.systemPrompt());
        return map;
    }

    private SourceDocument toSourceDocument(RagScoredChunk scoredChunk) {
        Map<String, String> metadata = new HashMap<>();
        scoredChunk.chunk().metadata()
                .forEach((key, value) -> metadata.put(key, String.valueOf(value)));

        String source = metadata.getOrDefault("source", scoredChunk.chunk().documentId());
        return new SourceDocument(
                scoredChunk.chunk().id(),
                source,
                scoredChunk.chunk().text(),
                source,
                metadata,
                (float) scoredChunk.score(),
                -1,
                "");
    }

    private float averageScore(List<RagScoredChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0f;
        }
        double sum = chunks.stream().mapToDouble(RagScoredChunk::score).sum();
        return (float) (sum / chunks.size());
    }
}
