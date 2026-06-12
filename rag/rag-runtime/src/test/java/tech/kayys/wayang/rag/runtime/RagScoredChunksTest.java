package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagScoredChunksTest {

    @Test
    void defaultsNullInputsToEmptyLists() {
        assertEquals(List.of(), RagScoredChunks.fromResult(null));
        assertEquals(List.of(), RagScoredChunks.valid(null));
    }

    @Test
    void filtersMalformedChunksAndPreservesOrder() {
        RagScoredChunk first = scored("a", 0.2);
        RagScoredChunk second = scored("b", 0.7);

        List<RagScoredChunk> result = RagScoredChunks.valid(Arrays.asList(
                null,
                first,
                new RagScoredChunk(null, 0.9),
                second));

        assertEquals(List.of(first, second), result);
    }

    @Test
    void extractsValidChunksFromResult() {
        RagScoredChunk chunk = scored("a", 0.4);
        RagResult result = new RagResult(RagQuery.of("question"), List.of(chunk), "answer", Map.of());

        assertEquals(List.of(chunk), RagScoredChunks.fromResult(result));
    }

    @Test
    void calculatesAverageScoreAcrossValidChunks() {
        assertEquals(0f, RagScoredChunks.averageScore(null));
        assertEquals(
                0.45f,
                RagScoredChunks.averageScore(Arrays.asList(
                        scored("a", 0.2),
                        new RagScoredChunk(null, 1.0),
                        scored("b", 0.7))),
                0.0001f);
    }

    private static RagScoredChunk scored(String id, double score) {
        return new RagScoredChunk(RagChunk.of("doc-" + id, 0, "content " + id, Map.of()), score);
    }
}
