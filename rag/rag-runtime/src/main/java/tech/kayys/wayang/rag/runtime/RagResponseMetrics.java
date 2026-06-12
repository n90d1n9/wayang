package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagMetrics;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

final class RagResponseMetrics {

    private RagResponseMetrics() {
    }

    static RagMetrics fromChunks(List<RagScoredChunk> chunks, int sourceDocumentCount) {
        List<RagScoredChunk> validChunks = RagScoredChunks.valid(chunks);
        return new RagMetrics(
                0L,
                sourceDocumentCount,
                0,
                RagScoredChunks.averageScore(validChunks),
                validChunks.size(),
                0,
                true);
    }
}
