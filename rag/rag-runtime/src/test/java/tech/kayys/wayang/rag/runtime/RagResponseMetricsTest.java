package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetrics;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResponseMetricsTest {

    @Test
    void buildsResponseMetricsFromScoredChunks() {
        RagMetrics metrics = RagResponseMetrics.fromChunks(Arrays.asList(
                scored("a", 0.2),
                scored("b", 0.8)),
                2);

        assertEquals(0L, metrics.totalDurationMs());
        assertEquals(2, metrics.documentsRetrieved());
        assertEquals(0, metrics.tokensGenerated());
        assertEquals(0.5f, metrics.averageSimilarityScore(), 0.0001f);
        assertEquals(2, metrics.rerankedResults());
        assertEquals(0, metrics.hallucinationScore());
        assertTrue(metrics.groundingVerified());
    }

    @Test
    void ignoresMalformedChunksBeforeScoringAndCountingRerankedResults() {
        RagMetrics metrics = RagResponseMetrics.fromChunks(Arrays.asList(
                null,
                scored("a", 0.4),
                new RagScoredChunk(null, 1.0)),
                1);

        assertEquals(1, metrics.documentsRetrieved());
        assertEquals(1, metrics.rerankedResults());
        assertEquals(0.4f, metrics.averageSimilarityScore(), 0.0001f);
    }

    @Test
    void defaultsEmptyMetricsWhenNoChunksAreAvailable() {
        RagMetrics metrics = RagResponseMetrics.fromChunks(null, 0);

        assertEquals(0, metrics.documentsRetrieved());
        assertEquals(0, metrics.rerankedResults());
        assertEquals(0f, metrics.averageSimilarityScore(), 0.0001f);
        assertTrue(metrics.groundingVerified());
    }

    private static RagScoredChunk scored(String id, double score) {
        return new RagScoredChunk(RagChunk.of("doc-" + id, 0, "content " + id, Map.of()), score);
    }
}
