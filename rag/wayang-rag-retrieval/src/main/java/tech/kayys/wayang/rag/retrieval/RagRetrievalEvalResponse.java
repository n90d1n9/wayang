package tech.kayys.wayang.rag.retrieval;

import java.time.Instant;
import java.util.List;

public record RagRetrievalEvalResponse(
        String datasetName,
        String tenantId,
        int topK,
        double minSimilarity,
        String matchField,
        int queryCount,
        int hitCount,
        double recallAtK,
        double mrr,
        double latencyP95Ms,
        double latencyAvgMs,
        Instant evaluatedAt,
        List<RagRetrievalEvalCaseResult> results) {
}
