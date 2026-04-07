package tech.kayys.wayang.rag.runtime;
import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainClassesTest {

    @Test
    void testRetrievalConfig_Creation() {
        // Given
        RetrievalConfig config = new RetrievalConfig(10, 0.75f, 1024, 100, true,
                RerankingModel.COHERE_RERANK, true, 0.6f,
                true, 5, false, 0, Map.of("key", "value"),
                List.of("field"), true, true);

        // When & Then
        assertEquals(10, config.topK());
        assertEquals(0.75f, config.minSimilarity());
        assertEquals(1024, config.maxChunkSize());
        assertEquals(100, config.chunkOverlap());
        assertTrue(config.enableReranking());
        assertEquals(RerankingModel.COHERE_RERANK, config.rerankingModel());
        assertTrue(config.enableHybridSearch());
        assertEquals(0.6f, config.hybridAlpha());
        assertTrue(config.enableMultiQuery());
        assertEquals(5, config.numQueryVariations());
        assertFalse(config.enableMmr());
        assertEquals(0, config.mmrLambda());
        assertEquals(Map.of("key", "value"), config.metadataFilters());
        assertEquals(List.of("field"), config.excludedFields());
        assertTrue(config.enableGrouping());
        assertTrue(config.enableDeduplication());
    }

    @Test
    void testRetrievalConfig_Defaults() {
        // When
        RetrievalConfig config = RetrievalConfig.defaults();

        // Then
        assertEquals(5, config.topK());
        assertEquals(0.5f, config.minSimilarity());
        assertEquals(512, config.maxChunkSize());
        assertEquals(50, config.chunkOverlap());
        assertFalse(config.enableReranking());
        assertEquals(RerankingModel.COHERE_RERANK, config.rerankingModel());
        assertFalse(config.enableHybridSearch());
        assertEquals(0.7f, config.hybridAlpha());
        assertFalse(config.enableMultiQuery());
        assertEquals(3, config.numQueryVariations());
    }

    @Test
    void testGenerationConfig_Creation() {
        // Given
        GenerationConfig config = new GenerationConfig("anthropic", "claude-3", 0.5f, 2048,
                0.9f, 0.1f, 0.1f, List.of("STOP"),
                "Custom system prompt", Map.of("param", "value"),
                true, true, CitationStyle.APA, true, true,
                Map.of("safety", "setting"));

        // When & Then
        assertEquals("anthropic", config.provider());
        assertEquals("claude-3", config.model());
        assertEquals(0.5f, config.temperature());
        assertEquals(2048, config.maxTokens());
        assertEquals(0.9f, config.topP());
        assertEquals(0.1f, config.frequencyPenalty());
        assertEquals(0.1f, config.presencePenalty());
        assertEquals(List.of("STOP"), config.stopSequences());
        assertEquals("Custom system prompt", config.systemPrompt());
        assertEquals(Map.of("param", "value"), config.additionalParams());
        assertTrue(config.enableCitations());
        assertTrue(config.enableGrounding());
        assertEquals(CitationStyle.APA, config.citationStyle());
        assertTrue(config.enableFactualityChecks());
        assertTrue(config.enableBiasDetection());
        assertEquals(Map.of("safety", "setting"), config.safetySettings());
    }

    @Test
    void testGenerationConfig_Defaults() {
        // When
        GenerationConfig config = GenerationConfig.defaults();

        // Then
        assertEquals("openai", config.provider());
        assertEquals("gpt-4", config.model());
        assertEquals(0.7f, config.temperature());
        assertEquals(1024, config.maxTokens());
        assertEquals(1.0f, config.topP());
        assertEquals(0.0f, config.frequencyPenalty());
        assertEquals(0.0f, config.presencePenalty());
        assertEquals(List.of(), config.stopSequences());
        assertEquals("You are a helpful assistant.", config.systemPrompt());
        assertEquals(Map.of(), config.additionalParams());
        assertFalse(config.enableCitations());
        assertFalse(config.enableGrounding());
        assertEquals(CitationStyle.INLINE_NUMBERED, config.citationStyle());
        assertFalse(config.enableFactualityChecks());
        assertFalse(config.enableBiasDetection());
        assertEquals(Map.of(), config.safetySettings());
    }

    @Test
    void testRagResponse_Creation() {
        // Given
        RagResponse response = new RagResponse(
                "test query",
                "test answer",
                List.of(new SourceDocument("id1", "title1", "content1", "uri1", Map.of(), 0.9f, 1, "section1")),
                List.of(new Citation(1, "citation content", "uri1", "title1", 1, "section1", 0.8f)),
                new RagMetrics(100, 5, 100, 0.85f, 3, 5, true),
                "context content",
                Instant.now(),
                Map.of("meta", "data"),
                List.of("source1"),
                java.util.Optional.empty());

        // When & Then
        assertEquals("test query", response.query());
        assertEquals("test answer", response.answer());
        assertEquals(1, response.sourceDocuments().size());
        assertEquals(1, response.citations().size());
        assertNotNull(response.metrics());
        assertEquals("context content", response.context());
        assertNotNull(response.timestamp());
        assertEquals(Map.of("meta", "data"), response.metadata());
        assertEquals(List.of("source1"), response.sources());
        assertFalse(response.error().isPresent());
    }

    @Test
    void testRagWorkflowInput_Creation() {
        // Given
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        GenerationConfig generationConfig = GenerationConfig.defaults();

        RagWorkflowInput input = new RagWorkflowInput("tenant1", "query1", retrievalConfig, generationConfig);

        // When & Then
        assertEquals("tenant1", input.tenantId());
        assertEquals("query1", input.query());
        assertEquals(retrievalConfig, input.retrievalConfig());
        assertEquals(generationConfig, input.generationConfig());
    }

    @Test
    void testChunkingConfig_Creation() {
        // Given
        ChunkingConfig config = new ChunkingConfig(1024, 100);

        // When & Then
        assertEquals(1024, config.chunkSize());
        assertEquals(100, config.chunkOverlap());
    }

    @Test
    void testChunkingConfig_Defaults() {
        // When
        ChunkingConfig config = ChunkingConfig.defaults();

        // Then
        assertEquals(512, config.chunkSize());
        assertEquals(50, config.chunkOverlap());
    }

    @Test
    void testRagMetrics_Creation() {
        // Given
        RagMetrics metrics = new RagMetrics(500, 10, 200, 0.9f, 5, 2, true);

        // When & Then
        assertEquals(500, metrics.totalDurationMs());
        assertEquals(10, metrics.documentsRetrieved());
        assertEquals(200, metrics.tokensGenerated());
        assertEquals(0.9f, metrics.averageSimilarityScore());
        assertEquals(5, metrics.rerankedResults());
        assertEquals(2, metrics.hallucinationScore());
        assertTrue(metrics.groundingVerified());
    }

    @Test
    void testSourceDocument_Creation() {
        // Given
        SourceDocument doc = new SourceDocument("id1", "title1", "content1", "uri1",
                Map.of("key", "value"), 0.85f, 1, "section1");

        // When & Then
        assertEquals("id1", doc.getId());
        assertEquals("title1", doc.getTitle());
        assertEquals("content1", doc.getContent());
        assertEquals("uri1", doc.getSourceUri());
        assertEquals(Map.of("key", "value"), doc.getMetadata());
        assertEquals(0.85f, doc.getSimilarityScore());
        assertEquals(1, doc.getPageNumber());
        assertEquals("section1", doc.getSectionTitle());
    }

    @Test
    void testCitation_Creation() {
        // Given
        Citation citation = new Citation(1, "citation content", "uri1", "title1", 1, "section1", 0.9f);

        // When & Then
        assertEquals(1, citation.getIndex());
        assertEquals("citation content", citation.getContent());
        assertEquals("uri1", citation.getSourceUri());
        assertEquals("title1", citation.getTitle());
        assertEquals(1, citation.getPageNumber());
        assertEquals("section1", citation.getSectionTitle());
        assertEquals(0.9f, citation.getConfidenceScore());
    }
}
