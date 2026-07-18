package tech.kayys.wayang.agent.spi.skills.rag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider-neutral document returned by the RAG skill retrieval boundary.
 */
public record RagSkillRetrievedDocument(
        String id,
        String title,
        String content,
        String source,
        double score,
        Map<String, Object> metadata) {

    public RagSkillRetrievedDocument {
        id = trimToEmpty(id);
        title = trimToEmpty(title);
        content = trimToEmpty(content);
        source = trimToEmpty(source);
        metadata = copyMetadata(metadata);
    }

    public Map<String, Object> sourceSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", id);
        summary.put("title", title);
        summary.put("source", source);
        summary.put("score", score);
        summary.put("metadata", metadata);
        return Map.copyOf(summary);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                copied.put(key, value);
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }
}
