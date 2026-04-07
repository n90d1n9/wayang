package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

        @Mock
        private NativeRagCoreService nativeRagCoreService;

        @Test
        void testCompleteRagFlow() {
                // This is a high-level integration test demonstrating how components work
                // together

                // Given
                String tenantId = "integration-test-tenant";
                String query = "What is the meaning of life?";

                // Create services (in a real test, we'd properly mock dependencies)
                DocumentIngestionService ingestionService = new DocumentIngestionService();
                RagQueryService queryService = new RagQueryService();
                ingestionService.nativeRagCoreService = nativeRagCoreService;
                queryService.nativeRagCoreService = nativeRagCoreService;

                when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                                any(ChunkingConfig.class)))
                                .thenReturn(List.of(RagChunk.of("doc", 0, "chunk", Map.of("source", "s"))));
                when(nativeRagCoreService.query(anyString(), anyString(), any(RetrievalConfig.class),
                                any(GenerationConfig.class), anyMap()))
                                .thenReturn(new RagResult(
                                                RagQuery.of("q"),
                                                List.of(new RagScoredChunk(
                                                                RagChunk.of("doc", 0, "chunk", Map.of("source", "s")),
                                                                0.9)),
                                                "answer",
                                                Map.of()));

                // Create a sample document source
                DocumentSource source = new DocumentSource(
                                SourceType.TEXT,
                                null,
                                "Life is a characteristic that distinguishes physical entities that have biological processes from those that do not.",
                                Map.of("topic", "philosophy"));

                // When - Test document ingestion
                IngestResult ingestResult = ingestionService.ingestTextDocuments(
                                tenantId,
                                List.of(source.content()),
                                source.metadata(),
                                ChunkingConfig.defaults())
                                .await().indefinitely();

                // Then - Verify ingestion result structure
                assertNotNull(ingestResult);
                assertEquals(1, ingestResult.documentsIngested());

                // When - Test query creation
                RagQueryRequest request = new RagQueryRequest(
                                tenantId,
                                query,
                                RagMode.STANDARD,
                                SearchStrategy.HYBRID,
                                RetrievalConfig.defaults(),
                                GenerationConfig.defaults(),
                                List.of("philosophy-docs"),
                                Map.of());

                // Then - Verify request structure
                assertEquals(tenantId, request.tenantId());
                assertEquals(query, request.query());
                assertEquals(RagMode.STANDARD, request.ragMode());
                RagResponse response = queryService.advancedQuery(request).await().indefinitely();
                assertNotNull(response);
                assertEquals("answer", response.answer());

                // When - Test conversation turn creation
                ConversationTurn turn = new ConversationTurn(
                                query,
                                "The meaning of life is subjective and varies between individuals and cultures.",
                                Instant.now());

                // Then - Verify conversation turn structure
                assertEquals(query, turn.role());
                assertFalse(turn.content().isEmpty());
                assertNotNull(turn.timestamp());
        }

        @Test
        void testRecordEqualityAndHashCode() {
                // Test equality and hash code for records

                // IngestResult
                IngestResult result1 = new IngestResult(true, 1, 10, 100L, "Success");
                IngestResult result2 = new IngestResult(true, 1, 10, 100L, "Success");
                IngestResult result3 = new IngestResult(false, 1, 10, 100L, "Success");

                assertEquals(result1, result2);
                assertNotEquals(result1, result3);
                assertEquals(result1.hashCode(), result2.hashCode());

                // DocumentSource
                DocumentSource source1 = new DocumentSource(SourceType.PDF, "/doc1.pdf", null, Map.of("key", "value"));
                DocumentSource source2 = new DocumentSource(SourceType.PDF, "/doc1.pdf", null, Map.of("key", "value"));
                DocumentSource source3 = new DocumentSource(SourceType.TEXT, "/doc1.pdf", null, Map.of("key", "value"));

                assertEquals(source1, source2);
                assertNotEquals(source1, source3);
                assertEquals(source1.hashCode(), source2.hashCode());

                // RagQueryRequest
                RagQueryRequest request1 = new RagQueryRequest(
                                "tenant1", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
                                RetrievalConfig.defaults(), GenerationConfig.defaults(),
                                List.of("col1"), Map.of("filter", "value"));
                RagQueryRequest request2 = new RagQueryRequest(
                                "tenant1", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
                                RetrievalConfig.defaults(), GenerationConfig.defaults(),
                                List.of("col1"), Map.of("filter", "value"));
                RagQueryRequest request3 = new RagQueryRequest(
                                "tenant2", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
                                RetrievalConfig.defaults(), GenerationConfig.defaults(),
                                List.of("col1"), Map.of("filter", "value"));

                assertEquals(request1.tenantId(), request2.tenantId());
                assertEquals(request1.query(), request2.query());
                assertNotEquals(request1, request3);

                // ConversationTurn
                Instant now = Instant.now();
                ConversationTurn turn1 = new ConversationTurn("msg1", "msg2", now);
                ConversationTurn turn2 = new ConversationTurn("msg1", "msg2", now);
                ConversationTurn turn3 = new ConversationTurn("msg1", "msg3", now);

                assertEquals(turn1, turn2);
                assertNotEquals(turn1, turn3);
                assertEquals(turn1.hashCode(), turn2.hashCode());
        }
}
