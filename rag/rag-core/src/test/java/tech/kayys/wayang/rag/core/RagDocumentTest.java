package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagDocumentTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "doc.md");
        metadata.put("nullable", null);

        RagDocument document = RagDocument.of("doc", "content", metadata);
        metadata.put("source", "mutated");

        assertEquals("doc.md", document.metadata().get("source"));
        assertTrue(document.metadata().containsKey("nullable"));
        assertNull(document.metadata().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> document.metadata().put("other", "value"));
    }
}
