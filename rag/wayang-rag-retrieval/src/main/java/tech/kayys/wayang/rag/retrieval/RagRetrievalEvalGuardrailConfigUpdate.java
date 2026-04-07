package tech.kayys.wayang.rag.retrieval;

public record RagRetrievalEvalGuardrailConfigUpdate(
        Boolean enabled,
        Integer windowSize,
        Double recallDropMax,
        Double mrrDropMax,
        Double latencyP95IncreaseMaxMs,
        Double latencyAvgIncreaseMaxMs) {
}
