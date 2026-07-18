package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceDocumentTest {

    @Test
    void copiesMetadataDefensivelyAndSkipsNullStringEntries() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("collection", "docs");
        metadata.put("ignored", null);
        metadata.put(null, "ignored");

        SourceDocument document = new SourceDocument(
                "doc-1",
                "title",
                "content",
                "uri",
                metadata,
                0.9f,
                3,
                "section");
        metadata.put("collection", "mutated");

        assertEquals(Map.of("collection", "docs"), document.getMetadata());
        assertThrows(UnsupportedOperationException.class, () -> document.getMetadata().put("other", "value"));
    }

    @Test
    void defaultsMissingMetadataToEmptyImmutableMap() {
        SourceDocument document = new SourceDocument(
                "doc-1",
                "title",
                "content",
                "uri",
                null,
                0.9f,
                3,
                "section");

        assertEquals(Map.of(), document.getMetadata());
        assertThrows(UnsupportedOperationException.class, () -> document.getMetadata().put("other", "value"));
    }
}
