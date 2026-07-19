package tech.kayys.wayang.rag.core.eval;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvaluationHarnessTest {

    @Test
    void shouldComputeRecallMrrAndLatency() {
        Retriever retriever = query -> {
            if ("q1".equals(query.text())) {
                return List.of(
                        new RagScoredChunk(chunk("a"), 0.9),
                        new RagScoredChunk(chunk("b"), 0.7));
            }
            if ("q2".equals(query.text())) {
                return List.of(
                        new RagScoredChunk(chunk("z"), 0.8),
                        new RagScoredChunk(chunk("y"), 0.6));
            }
            return List.of();
        };

        List<RagEvalCase> fixtures = List.of(
                new RagEvalCase("q1", List.of("a")),
                new RagEvalCase("q2", List.of("x", "y")));

        RagEvalResult result = new RagEvaluationHarness().evaluate(retriever, fixtures, 2);

        assertEquals(2, result.totalQueries());
        assertEquals(2, result.topK());
        assertEquals(0.75, result.recallAtK(), 0.0001);
        assertEquals(0.75, result.mrr(), 0.0001);
        assertTrue(result.latencyP95Ms() >= 0);
        assertTrue(result.latencyAvgMs() >= 0);
    }

    private static RagChunk chunk(String id) {
        return new RagChunk(id, "doc", 0, "text", Map.of());
    }
}
