package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationTest {

    @Test
    void defaultsMissingMetadataFields() {
        Citation citation = new Citation("content", "uri", 1, null);

        assertEquals(1, citation.getIndex());
        assertEquals("content", citation.getContent());
        assertEquals("uri", citation.getSourceUri());
        assertEquals("", citation.getTitle());
        assertEquals(-1, citation.getPageNumber());
        assertEquals("", citation.getSectionTitle());
        assertEquals(1.0f, citation.getConfidenceScore());
    }

    @Test
    void readsTypedMetadataFieldsFromSnapshot() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Guide");
        metadata.put("pageNumber", "7");
        metadata.put("sectionTitle", "Setup");
        metadata.put("confidenceScore", "0.85");

        Citation citation = new Citation("content", "uri", 2, metadata);
        metadata.put("title", "mutated");

        assertEquals("Guide", citation.getTitle());
        assertEquals(7, citation.getPageNumber());
        assertEquals("Setup", citation.getSectionTitle());
        assertEquals(0.85f, citation.getConfidenceScore());
    }
}
