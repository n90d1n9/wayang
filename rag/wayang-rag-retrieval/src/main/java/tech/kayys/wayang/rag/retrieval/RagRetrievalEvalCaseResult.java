package tech.kayys.wayang.rag.retrieval;

import java.util.List;

public record RagRetrievalEvalCaseResult(
        String caseId,
        String query,
        List<String> expectedIds,
        List<String> retrievedIds,
        double recall,
        double reciprocalRank,
        long latencyMs) {
}
