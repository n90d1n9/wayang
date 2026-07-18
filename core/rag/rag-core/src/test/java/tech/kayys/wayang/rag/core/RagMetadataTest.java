package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagMetadataTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "doc.md");
        metadata.put("nullable", null);

        Map<String, Object> copied = RagMetadata.copy(metadata);
        metadata.put("source", "mutated");

        assertEquals("doc.md", copied.get("source"));
        assertTrue(copied.containsKey("nullable"));
        assertNull(copied.get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void copiesStringMetadataDefensivelyAndSkipsNullEntries() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("collection", "docs");
        metadata.put("ignored", null);
        metadata.put(null, "ignored");

        Map<String, String> copied = RagMetadata.copyStrings(metadata);
        metadata.put("collection", "mutated");

        assertEquals(Map.of("collection", "docs"), copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }
}
