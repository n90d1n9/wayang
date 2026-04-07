package tech.kayys.wayang.rag.runtime;
import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.SearchStrategy;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceTest {

    @Mock
    private NativeRagCoreService nativeRagCoreService;

    private RagQueryService ragQueryService;

    @BeforeEach
    void setUp() {
        ragQueryService = new RagQueryService();
        ragQueryService.nativeRagCoreService = nativeRagCoreService;
    }

    @Test
    void testQuery_Success() {
        when(nativeRagCoreService.query(anyString(), anyString(), any(RetrievalConfig.class),
                any(GenerationConfig.class), anyMap()))
                .thenReturn(fakeResult("answer-1"));

        RagResponse response = ragQueryService.query("tenant", "question", "col").await().indefinitely();

        assertNotNull(response);
        assertEquals("answer-1", response.answer());
        assertEquals(1, response.sourceDocuments().size());
    }

    @Test
    void testAdvancedQuery_Success() {
        when(nativeRagCoreService.query(anyString(), anyString(), any(RetrievalConfig.class),
                any(GenerationConfig.class), anyMap()))
                .thenReturn(fakeResult("answer-2"));

        RagQueryRequest request = new RagQueryRequest(
                "tenant",
                "question",
                RagMode.STANDARD,
                SearchStrategy.HYBRID,
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                List.of("docs"),
                Map.of("collection", "docs"));

        RagResponse response = ragQueryService.advancedQuery(request).await().indefinitely();

        assertNotNull(response);
        assertEquals("answer-2", response.answer());
    }

    @Test
    void testConversationalQuery_Success() {
        when(nativeRagCoreService.query(anyString(), anyString(), any(RetrievalConfig.class),
                any(GenerationConfig.class), anyMap()))
                .thenReturn(fakeResult("answer-3"));

        RagResponse response = ragQueryService.conversationalQuery(
                "tenant",
                "question",
                "session-1",
                List.of(new ConversationTurn("u", "a", Instant.now())))
                .await().indefinitely();

        assertNotNull(response);
        assertEquals("answer-3", response.answer());
    }

    private RagResult fakeResult(String answer) {
        RagChunk chunk = RagChunk.of("doc-1", 0, "content", Map.of("source", "s1"));
        return new RagResult(
                RagQuery.of("q"),
                List.of(new RagScoredChunk(chunk, 0.9)),
                answer,
                Map.of());
    }
}
