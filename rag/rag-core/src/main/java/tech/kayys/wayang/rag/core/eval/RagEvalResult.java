package tech.kayys.wayang.rag.core.eval;

public record RagEvalResult(
        int totalQueries,
        int topK,
        double recallAtK,
        double mrr,
        long latencyP95Ms,
        long latencyAvgMs) {
}
