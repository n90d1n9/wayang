package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagChunkTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "doc.md");
        metadata.put("nullable", null);

        RagChunk chunk = new RagChunk("chunk", "doc", 0, "text", metadata);
        metadata.put("source", "mutated");

        assertEquals("doc.md", chunk.metadata().get("source"));
        assertTrue(chunk.metadata().containsKey("nullable"));
        assertNull(chunk.metadata().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> chunk.metadata().put("other", "value"));
    }
}
