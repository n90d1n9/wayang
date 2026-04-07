package tech.kayys.wayang.rag.retrieval;

import java.time.Instant;
import java.util.List;

public record RagRetrievalEvalGuardrailStatus(
        boolean enabled,
        boolean healthy,
        String tenantId,
        String datasetName,
        int windowSize,
        int runCount,
        String reason,
        List<RagRetrievalEvalGuardrailBreach> breaches,
        RagRetrievalEvalTrendResponse trend,
        Instant evaluatedAt) {
}
