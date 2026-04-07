package tech.kayys.wayang.rag.retrieval;

public record RagRetrievalEvalGuardrailConfig(
        boolean enabled,
        int windowSize,
        double recallDropMax,
        double mrrDropMax,
        double latencyP95IncreaseMaxMs,
        double latencyAvgIncreaseMaxMs) {
}
