package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;
import tech.kayys.wayang.rag.core.SourceDocument;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagQueryResponseAssemblerTest {

    @Test
    void assemblesResponseMetadataSourcesAndMetrics() {
        RagQueryWorkflowContext context = RagQueryWorkflowContext.fromRequest(new RagQueryRequest(
                "tenant",
                "question",
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of("docs"),
                Map.of()));
        RagChunk chunk = RagChunk.of(
                "doc-1",
                0,
                "content",
                Map.of(RagMetadataKeys.SOURCE, "manual", "page", 2));
        RagResult result = new RagResult(
                RagQuery.of("question"),
                List.of(new RagScoredChunk(chunk, 0.8)),
                "answer",
                Map.of());

        RagResponse response = RagQueryResponseAssembler.toResponse(
                context,
                result,
                RagScoredChunks.fromResult(result));

        SourceDocument source = response.sourceDocuments().getFirst();
        assertEquals("question", response.query());
        assertEquals("answer", response.answer());
        assertEquals(List.of("docs"), response.sources());
        assertEquals("manual", source.getTitle());
        assertEquals("manual", source.getSourceUri());
        assertEquals("2", source.getMetadata().get("page"));
        assertEquals(1, response.metrics().documentsRetrieved());
        assertEquals(1, response.metrics().rerankedResults());
        assertEquals(0.8f, response.metrics().averageSimilarityScore(), 0.0001f);
        assertEquals(RagMode.STANDARD.name(), response.metadata().get(RagResponseMetadata.RAG_MODE));
        assertEquals(SearchStrategy.HYBRID.name(), response.metadata().get(RagResponseMetadata.SEARCH_STRATEGY));
        assertEquals(
                NativeGenerationMode.CONTEXT.name(),
                ((Map<?, ?>) response.metadata().get(RagResponseMetadata.GENERATION_CONFIG))
                        .get(NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE));
    }

    @Test
    void filtersNullChunksBeforeBuildingResponse() {
        RagQueryWorkflowContext context = RagQueryWorkflowContext.simple("tenant", "question", null);
        RagChunk chunk = RagChunk.of("doc-1", 0, "content", Map.of());
        RagResult result = new RagResult(
                RagQuery.of("question"),
                Arrays.asList(null, new RagScoredChunk(null, 0.4), new RagScoredChunk(chunk, 0.6)),
                "answer",
                Map.of());

        RagResponse response = RagQueryResponseAssembler.toResponse(
                context,
                result,
                RagScoredChunks.fromResult(result));

        assertEquals(1, response.sourceDocuments().size());
        assertEquals(1, response.metrics().documentsRetrieved());
        assertEquals(0.6f, response.metrics().averageSimilarityScore(), 0.0001f);
    }
}
