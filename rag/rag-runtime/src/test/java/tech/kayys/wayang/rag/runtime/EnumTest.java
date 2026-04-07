package tech.kayys.wayang.rag.runtime;
import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTest {

    @Test
    void testSourceType_Values() {
        // Test all values exist
        SourceType[] values = SourceType.values();
        assertEquals(5, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(SourceType.PDF));
        assertTrue(java.util.Arrays.asList(values).contains(SourceType.TEXT));
        assertTrue(java.util.Arrays.asList(values).contains(SourceType.URL));
        assertTrue(java.util.Arrays.asList(values).contains(SourceType.MARKDOWN));
        assertTrue(java.util.Arrays.asList(values).contains(SourceType.HTML));
    }

    @Test
    void testRagMode_Values() {
        // Test all values exist
        RagMode[] values = RagMode.values();
        assertEquals(4, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(RagMode.STANDARD));
        assertTrue(java.util.Arrays.asList(values).contains(RagMode.AGENT));
        assertTrue(java.util.Arrays.asList(values).contains(RagMode.MULTI_HOP));
        assertTrue(java.util.Arrays.asList(values).contains(RagMode.HYBRID));
    }

    @Test
    void testSearchStrategy_Values() {
        // Test all values exist
        SearchStrategy[] values = SearchStrategy.values();
        assertEquals(4, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(SearchStrategy.SEMANTIC));
        assertTrue(java.util.Arrays.asList(values).contains(SearchStrategy.HYBRID));
        assertTrue(java.util.Arrays.asList(values).contains(SearchStrategy.SEMANTIC_RERANK));
        assertTrue(java.util.Arrays.asList(values).contains(SearchStrategy.MULTI_QUERY));
    }

    @Test
    void testChunkingStrategy_Values() {
        // Test all values exist
        ChunkingStrategy[] values = ChunkingStrategy.values();
        assertEquals(3, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(ChunkingStrategy.RECURSIVE));
        assertTrue(java.util.Arrays.asList(values).contains(ChunkingStrategy.SENTENCE));
        assertTrue(java.util.Arrays.asList(values).contains(ChunkingStrategy.PARAGRAPH));
    }

    @Test
    void testRerankingModel_Values() {
        // Test all values exist
        RerankingModel[] values = RerankingModel.values();
        assertEquals(3, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(RerankingModel.COHERE_RERANK));
        assertTrue(java.util.Arrays.asList(values).contains(RerankingModel.JINA_AI_RERANK));
        assertTrue(java.util.Arrays.asList(values).contains(RerankingModel.MIXEDBREAD_RERANK));
    }

    @Test
    void testCitationStyle_Values() {
        // Test all values exist
        CitationStyle[] values = CitationStyle.values();
        assertEquals(4, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(CitationStyle.INLINE_NUMBERED));
        assertTrue(java.util.Arrays.asList(values).contains(CitationStyle.FOOTNOTE));
        assertTrue(java.util.Arrays.asList(values).contains(CitationStyle.APA));
        assertTrue(java.util.Arrays.asList(values).contains(CitationStyle.MLA));
    }

    @Test
    void testEnumToString() {
        // Test that toString returns the enum name
        assertEquals("PDF", SourceType.PDF.toString());
        assertEquals("STANDARD", RagMode.STANDARD.toString());
        assertEquals("SEMANTIC", SearchStrategy.SEMANTIC.toString());
        assertEquals("RECURSIVE", ChunkingStrategy.RECURSIVE.toString());
        assertEquals("COHERE_RERANK", RerankingModel.COHERE_RERANK.toString());
        assertEquals("INLINE_NUMBERED", CitationStyle.INLINE_NUMBERED.toString());
    }
}
