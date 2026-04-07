package tech.kayys.wayang.rag.core;

import java.util.Map;

public record RagQuery(
        String text,
        int topK,
        double minScore,
        Map<String, Object> filters) {

    public static RagQuery of(String text) {
        return new RagQuery(text, 5, 0.0, Map.of());
    }
}
