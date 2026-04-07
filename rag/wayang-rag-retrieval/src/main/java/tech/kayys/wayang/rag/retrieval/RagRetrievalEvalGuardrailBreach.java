package tech.kayys.wayang.rag.retrieval;

public record RagRetrievalEvalGuardrailBreach(
        String metric,
        double observedDelta,
        double threshold,
        String message) {
}
