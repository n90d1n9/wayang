package tech.kayys.wayang.rag.core;

import java.util.List;
import java.util.Map;

public record RagEvalQueryCase(
        String id,
        String query,
        List<String> expectedIds,
        Map<String, Object> filters) {
}
