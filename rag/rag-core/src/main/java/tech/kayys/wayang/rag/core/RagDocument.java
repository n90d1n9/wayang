package tech.kayys.wayang.rag.core;

import java.util.Map;

public record RagDocument(
        String id,
        String content,
        Map<String, Object> metadata) {

    public static RagDocument of(String content, Map<String, Object> metadata) {
        return new RagDocument(java.util.UUID.randomUUID().toString(), content,
                metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
