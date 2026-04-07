package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.SearchStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    void testIngestResult_Record() {
        // Given
        boolean success = true;
        int documentsIngested = 5;
        int segmentsCreated = 50;
        long durationMs = 1000L;
        String message = "Test message";

        // When
        IngestResult result = new IngestResult(success, documentsIngested, segmentsCreated, durationMs, message);

        // Then
        assertTrue(result.success());
        assertEquals(documentsIngested, result.documentsIngested());
        assertEquals(segmentsCreated, result.segmentsCreated());
        assertEquals(durationMs, result.durationMs());
        assertEquals(message, result.message());
    }

    @Test
    void testDocumentSource_Record() {
        // Given
        SourceType type = SourceType.PDF;
        String path = "/path/to/document.pdf";
        String content = "Document content";
        Map<String, String> metadata = Map.of("key", "value");

        // When
        DocumentSource source = new DocumentSource(type, path, content, metadata);

        // Then
        assertEquals(type, source.type());
        assertEquals(path, source.path());
        assertEquals(content, source.content());
        assertEquals(metadata, source.metadata());
    }

    @Test
    void testRagQueryRequest_Record() {
        // Given
        String tenantId = "test-tenant";
        String query = "test query";
        RagMode ragMode = RagMode.STANDARD;
        SearchStrategy searchStrategy = SearchStrategy.HYBRID;
        var retrievalConfig = tech.kayys.wayang.rag.core.RetrievalConfig.defaults();
        var generationConfig = tech.kayys.wayang.rag.core.GenerationConfig.defaults();
        List<String> collections = List.of("collection1", "collection2");
        Map<String, Object> filters = Map.of("filter", "value");

        // When
        RagQueryRequest request = new RagQueryRequest(
                tenantId, query, ragMode, searchStrategy,
                retrievalConfig, generationConfig, collections, filters);

        // Then
        assertEquals(tenantId, request.tenantId());
        assertEquals(query, request.query());
        assertEquals(ragMode, request.ragMode());
        assertEquals(searchStrategy, request.searchStrategy());
        assertEquals(retrievalConfig, request.retrievalConfig());
        assertEquals(generationConfig, request.generationConfig());
        assertEquals(collections, request.collections());
        assertEquals(filters, request.filters());
    }

    @Test
    void testConversationTurn_Record() {
        // Given
        String userMessage = "User message";
        String assistantMessage = "Assistant message";
        Instant timestamp = Instant.now();

        // When
        ConversationTurn turn = new ConversationTurn(userMessage, assistantMessage, timestamp);

        // Then
        assertEquals(userMessage, turn.role());
        assertEquals(assistantMessage, turn.content());
        assertEquals(timestamp, turn.timestamp());
    }
}