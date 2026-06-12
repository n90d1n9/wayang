package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievedDocument;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalRequest;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalResult;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetriever;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SourceDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges agent RAG skill retrieval requests to the native RAG runtime.
 */
@ApplicationScoped
public class NativeRagSkillRetriever implements RagSkillRetriever {

    @Inject
    NativeRagCoreService nativeRagCoreService;

    @Override
    public Uni<RagSkillRetrievalResult> retrieve(RagSkillRetrievalRequest request) {
        if (request == null || request.query().isBlank() || nativeRagCoreService == null) {
            return Uni.createFrom().item(RagSkillRetrievalResult.empty());
        }
        return Uni.createFrom().item(() -> {
            List<RagScoredChunk> chunks = nativeRagCoreService.retrieve(
                    request.tenantId(),
                    request.query(),
                    retrievalConfig(request),
                    filters(request));
            return new RagSkillRetrievalResult(RagSourceDocuments.fromChunks(chunks).stream()
                    .map(NativeRagSkillRetriever::toRetrievedDocument)
                    .toList());
        });
    }

    private static RetrievalConfig retrievalConfig(RagSkillRetrievalRequest request) {
        RetrievalConfig defaults = RetrievalConfig.defaults();
        int topK = request.topK() > 0 ? request.topK() : defaults.topK();
        return new RetrievalConfig(
                topK,
                defaults.minSimilarity(),
                defaults.maxChunkSize(),
                defaults.chunkOverlap(),
                defaults.enableReranking(),
                defaults.rerankingModel(),
                defaults.enableHybridSearch(),
                defaults.hybridAlpha(),
                defaults.enableMultiQuery(),
                defaults.numQueryVariations(),
                defaults.enableMmr(),
                defaults.mmrLambda(),
                defaults.metadataFilters(),
                defaults.excludedFields(),
                defaults.enableGrouping(),
                defaults.enableDeduplication());
    }

    private static Map<String, Object> filters(RagSkillRetrievalRequest request) {
        Map<String, Object> filters = RagRuntimeMetadata.mutableCopy(request.filters());
        if (!request.collection().isBlank()) {
            filters.put(RagMetadataKeys.COLLECTION, request.collection());
        }
        return RagRuntimeMetadata.copy(filters);
    }

    private static RagSkillRetrievedDocument toRetrievedDocument(SourceDocument source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        source.getMetadata().forEach(metadata::put);
        return new RagSkillRetrievedDocument(
                source.getId(),
                source.getTitle(),
                source.getContent(),
                source.getSourceUri(),
                source.getSimilarityScore(),
                metadata);
    }
}
