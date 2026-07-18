package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResponseTest {

    @Test
    void copiesListsAndMetadataDefensively() {
        SourceDocument sourceDocument = new SourceDocument(
                "source-1",
                "title",
                "content",
                "uri",
                Map.of(),
                0.8f,
                1,
                "section");
        Citation citation = new Citation(1, "content", "uri", "title", 1, "section", 0.9f);
        List<SourceDocument> sourceDocuments = new ArrayList<>(List.of(sourceDocument));
        List<Citation> citations = new ArrayList<>(List.of(citation));
        List<String> sources = new ArrayList<>(List.of("uri"));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "native");
        metadata.put("nullable", null);

        RagResponse response = new RagResponse(
                "question",
                "answer",
                sourceDocuments,
                citations,
                null,
                "context",
                Instant.parse("2026-05-27T00:00:00Z"),
                metadata,
                sources,
                Optional.empty());
        sourceDocuments.clear();
        citations.clear();
        sources.clear();
        metadata.put("provider", "mutated");

        assertEquals(List.of(sourceDocument), response.sourceDocuments());
        assertEquals(List.of(citation), response.citations());
        assertEquals(List.of("uri"), response.sources());
        assertEquals("native", response.metadata().get("provider"));
        assertTrue(response.metadata().containsKey("nullable"));
        assertNull(response.metadata().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> response.sourceDocuments().add(sourceDocument));
        assertThrows(UnsupportedOperationException.class, () -> response.citations().add(citation));
        assertThrows(UnsupportedOperationException.class, () -> response.sources().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> response.metadata().put("other", "value"));
    }

    @Test
    void defaultsMissingCollectionsToEmptyImmutableValues() {
        RagResponse response = new RagResponse(
                "question",
                "answer",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals(List.of(), response.sourceDocuments());
        assertEquals(List.of(), response.citations());
        assertEquals(List.of(), response.sources());
        assertEquals(Map.of(), response.metadata());
        assertTrue(response.error().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> response.sourceDocuments().add(null));
        assertThrows(UnsupportedOperationException.class, () -> response.citations().add(null));
        assertThrows(UnsupportedOperationException.class, () -> response.sources().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> response.metadata().put("other", "value"));
    }
}
