package tech.kayys.wayang.rag.core;

import java.util.List;
import java.util.Map;

public record RagEvalDataset(
        String name,
        String tenantId,
        Integer topK,
        Double minSimilarity,
        String matchField,
        Map<String, Object> defaultFilters,
        List<RagEvalQueryCase> queries) {
}
