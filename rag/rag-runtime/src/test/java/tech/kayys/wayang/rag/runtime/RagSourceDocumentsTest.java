package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.SourceDocument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RagSourceDocumentsTest {

    @Test
    void projectsChunkMetadataToSourceDocument() {
        RagScoredChunk scoredChunk = new RagScoredChunk(
                RagChunk.of(
                        "doc-1",
                        0,
                        "content",
                        Map.of(RagMetadataKeys.SOURCE, "manual", "page", 2)),
                0.8);

        SourceDocument source = RagSourceDocuments.fromChunk(scoredChunk);

        assertEquals("manual", source.getTitle());
        assertEquals("manual", source.getSourceUri());
        assertEquals("content", source.getContent());
        assertEquals("2", source.getMetadata().get("page"));
        assertEquals(0.8f, source.getSimilarityScore(), 0.0001f);
    }

    @Test
    void fallsBackToDocumentIdWhenSourceMetadataIsMissing() {
        SourceDocument source = RagSourceDocuments.fromChunk(new RagScoredChunk(
                RagChunk.of("doc-1", 0, "content", Map.of()),
                0.4));

        assertEquals("doc-1", source.getTitle());
        assertEquals("doc-1", source.getSourceUri());
    }

    @Test
    void filtersMalformedChunksBeforeProjection() {
        RagScoredChunk valid = new RagScoredChunk(RagChunk.of("doc-1", 0, "content", Map.of()), 0.4);

        List<SourceDocument> sources = RagSourceDocuments.fromChunks(Arrays.asList(
                null,
                new RagScoredChunk(null, 0.9),
                valid));

        assertEquals(1, sources.size());
        assertEquals("doc-1", sources.getFirst().getTitle());
    }

    @Test
    void toleratesDirectChunksWithNullMetadata() {
        SourceDocument source = RagSourceDocuments.fromChunk(new RagScoredChunk(
                new RagChunk("chunk-1", "doc-1", 0, "content", null),
                0.3));

        assertEquals(Map.of(), source.getMetadata());
        assertEquals("doc-1", source.getTitle());
    }

    @Test
    void skipsNullMetadataEntriesWhenProjectingSources() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(RagMetadataKeys.SOURCE, "manual");
        metadata.put("page", 2);
        metadata.put("ignored", null);
        metadata.put(null, "ignored");

        SourceDocument source = RagSourceDocuments.fromChunk(new RagScoredChunk(
                RagChunk.of("doc-1", 0, "content", metadata),
                0.3));

        assertEquals(Map.of(RagMetadataKeys.SOURCE, "manual", "page", "2"), source.getMetadata());
        assertFalse(source.getMetadata().containsKey("ignored"));
    }
}
