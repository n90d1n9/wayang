package tech.kayys.wayang.rag.retrieval;

import java.time.Instant;
import java.util.List;

public record RagRetrievalEvalTrendResponse(
        String tenantId,
        String datasetName,
        int windowSize,
        int runCount,
        RagRetrievalEvalRun latest,
        RagRetrievalEvalRun previous,
        Double recallAtKDelta,
        Double mrrDelta,
        Double latencyP95MsDelta,
        Double latencyAvgMsDelta,
        List<RagRetrievalEvalRun> runs,
        Instant generatedAt) {
}
