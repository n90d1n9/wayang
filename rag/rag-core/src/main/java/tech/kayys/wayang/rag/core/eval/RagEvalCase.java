package tech.kayys.wayang.rag.core.eval;

import java.util.List;

public record RagEvalCase(
        String query,
        List<String> relevantChunkIds) {

    public RagEvalCase {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (relevantChunkIds == null || relevantChunkIds.isEmpty()) {
            throw new IllegalArgumentException("relevantChunkIds must not be empty");
        }
        if (relevantChunkIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("relevantChunkIds must not contain blank values");
        }
        relevantChunkIds = List.copyOf(relevantChunkIds);
    }
}
