package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagMetadataKeysTest {

    @Test
    void buildsEmbeddingScopeMetadata() {
        Map<String, Object> metadata = RagMetadataKeys.embeddingScope("tenant", "hash-2", 2, "v1");

        assertEquals("tenant", metadata.get(RagMetadataKeys.TENANT_ID));
        assertEquals("hash-2", metadata.get(RagMetadataKeys.EMBEDDING_MODEL));
        assertEquals(2, metadata.get(RagMetadataKeys.EMBEDDING_DIMENSION));
        assertEquals("v1", metadata.get(RagMetadataKeys.EMBEDDING_VERSION));
    }

    @Test
    void buildsIndexedChunkMetadata() {
        RagChunk chunk = RagChunk.of("doc-1", 3, "content", Map.of());

        Map<String, Object> metadata = RagMetadataKeys.indexedChunk("tenant", "hash-2", 2, "v1", chunk);

        assertEquals("tenant", metadata.get(RagMetadataKeys.TENANT_ID));
        assertEquals("doc-1", metadata.get(RagMetadataKeys.DOCUMENT_ID));
        assertEquals(3, metadata.get(RagMetadataKeys.CHUNK_INDEX));
    }

    @Test
    void rejectsMissingIndexedChunk() {
        assertThrows(
                NullPointerException.class,
                () -> RagMetadataKeys.indexedChunk("tenant", "hash-2", 2, "v1", null));
    }
}
