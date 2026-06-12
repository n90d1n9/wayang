package tech.kayys.wayang.rag.embedding;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.store.InMemoryVectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnedRagEmbeddingStoreAdapterTest {

    @Test
    void snapshotsMetadataAndPreservesNullableValues() {
        OwnedRagEmbeddingStoreAdapter adapter = new OwnedRagEmbeddingStoreAdapter(
                "tenant-a",
                "hash-2",
                2,
                new InMemoryVectorStore<RagChunk>());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "doc.md");
        metadata.put("nullable", null);

        adapter.add("chunk-1", new float[] { 1f, 0f }, "text", metadata);
        metadata.put("source", "mutated");
        Map<String, Object> filters = new HashMap<>();
        filters.put("nullable", null);

        List<RagEmbeddingMatch> matches = adapter.search(new float[] { 1f, 0f }, 5, 0.0, filters);

        assertEquals(1, matches.size());
        assertEquals("doc.md", matches.getFirst().metadata().get("source"));
        assertTrue(matches.getFirst().metadata().containsKey("nullable"));
        assertNull(matches.getFirst().metadata().get("nullable"));
    }
}
