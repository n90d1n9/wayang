package tech.kayys.wayang.rag.memory;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.memory.model.Memory;
import tech.kayys.wayang.memory.model.MemoryType;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagMemoryIntegrationTest {

    @Test
    void weightedRetrievalSplitsTopKAndAnnotatesMemoryChunks() {
        RecordingRetriever ragRetriever = new RecordingRetriever();
        Map<String, Object> memoryMetadata = new HashMap<>();
        memoryMetadata.put("topic", "deploy");
        memoryMetadata.put("nullable", null);
        RecordingMemoryRetriever memoryRetriever = new RecordingMemoryRetriever(List.of(memory(
                "mem-1",
                "profile",
                "Use blue deployments for rehearsals.",
                0.72,
                memoryMetadata)));
        RagMemoryIntegration integration = new RagMemoryIntegration(ragRetriever, memoryRetriever, 0.4);
        Map<String, Object> filters = new HashMap<>();
        filters.put("tenant", "tenant-a");
        filters.put("nullable", null);

        RagResult result = integration.retrieveWeighted("deploy plan", 5, filters)
                .await().indefinitely();
        filters.put("tenant", "mutated");
        memoryMetadata.put("topic", "mutated");

        assertEquals("deploy plan", ragRetriever.lastQuery.text());
        assertEquals(3, ragRetriever.lastQuery.topK());
        assertEquals("tenant-a", ragRetriever.lastQuery.filters().get("tenant"));
        assertTrue(ragRetriever.lastQuery.filters().containsKey("nullable"));
        assertNull(ragRetriever.lastQuery.filters().get("nullable"));
        assertEquals("deploy plan", memoryRetriever.lastQuery);
        assertEquals(2, memoryRetriever.lastTopK);
        assertEquals("tenant-a", memoryRetriever.lastFilters.get("tenant"));
        assertTrue(memoryRetriever.lastFilters.containsKey("nullable"));
        assertNull(memoryRetriever.lastFilters.get("nullable"));

        assertEquals(2, result.chunks().size());
        assertEquals(3, result.metadata().get("ragTopK"));
        assertEquals(2, result.metadata().get("memoryTopK"));
        assertEquals(1, result.metadata().get("ragChunkCount"));
        assertEquals(1, result.metadata().get("memoryChunkCount"));
        assertEquals(0.4, (double) result.metadata().get("memoryWeight"), 0.0001);

        RagChunk memoryChunk = result.chunks().get(1).chunk();
        assertEquals("mem-1", memoryChunk.id());
        assertEquals("memory:profile", memoryChunk.documentId());
        assertEquals("Use blue deployments for rehearsals.", memoryChunk.text());
        assertEquals("memory", memoryChunk.metadata().get("sourceType"));
        assertEquals("mem-1", memoryChunk.metadata().get("memoryId"));
        assertEquals("profile", memoryChunk.metadata().get("memoryNamespace"));
        assertEquals("SEMANTIC", memoryChunk.metadata().get("memoryType"));
        assertEquals("deploy", memoryChunk.metadata().get("topic"));
        assertTrue(memoryChunk.metadata().containsKey("nullable"));
        assertNull(memoryChunk.metadata().get("nullable"));
        assertEquals("2026-05-26T00:00:00Z", memoryChunk.metadata().get("memoryTimestamp"));
        assertEquals(0.72, result.chunks().get(1).score(), 0.0001);
    }

    @Test
    void clampsWeightAndSkipsRetrieversWhenTopKIsZero() {
        RecordingRetriever ragRetriever = new RecordingRetriever();
        RecordingMemoryRetriever memoryRetriever = new RecordingMemoryRetriever(List.of(memory(
                "mem-1",
                "profile",
                "Unused memory",
                0.5,
                Map.of())));
        RagMemoryIntegration integration = new RagMemoryIntegration(ragRetriever, memoryRetriever, Double.NaN);

        RagResult result = integration.retrieveWeighted(null, -1).await().indefinitely();

        assertEquals("", result.query().text());
        assertEquals(0, result.query().topK());
        assertEquals(0, result.chunks().size());
        assertEquals(0, result.metadata().get("ragTopK"));
        assertEquals(0, result.metadata().get("memoryTopK"));
        assertEquals(0.0, (double) result.metadata().get("memoryWeight"), 0.0001);
        assertFalse(ragRetriever.called);
        assertFalse(memoryRetriever.called);
    }

    private static Memory memory(
            String id,
            String namespace,
            String content,
            double importance,
            Map<String, Object> metadata) {
        return Memory.builder()
                .id(id)
                .namespace(namespace)
                .content(content)
                .type(MemoryType.SEMANTIC)
                .metadata(metadata)
                .timestamp(Instant.parse("2026-05-26T00:00:00Z"))
                .importance(importance)
                .build();
    }

    private static final class RecordingRetriever implements Retriever {
        private boolean called;
        private RagQuery lastQuery;

        @Override
        public List<RagScoredChunk> retrieve(RagQuery query) {
            called = true;
            lastQuery = query;
            return List.of(new RagScoredChunk(
                    new RagChunk("rag-1", "doc-1", 0, "Document chunk", Map.of("sourceType", "document")),
                    0.9));
        }
    }

    private static final class RecordingMemoryRetriever implements RagMemoryRetriever {
        private final List<Memory> memories;
        private boolean called;
        private String lastQuery;
        private int lastTopK;
        private Map<String, Object> lastFilters;

        private RecordingMemoryRetriever(List<Memory> memories) {
            this.memories = memories;
        }

        @Override
        public Uni<List<Memory>> retrieve(String query, int topK, Map<String, Object> filters) {
            called = true;
            lastQuery = query;
            lastTopK = topK;
            lastFilters = filters;
            return Uni.createFrom().item(memories);
        }
    }
}
