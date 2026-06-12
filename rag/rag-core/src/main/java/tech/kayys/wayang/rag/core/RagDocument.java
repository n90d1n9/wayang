package tech.kayys.wayang.rag.core;

import java.util.Map;

public record RagDocument(
        String id,
        String content,
        Map<String, Object> metadata) {

    public RagDocument {
        metadata = RagMetadata.copy(metadata);
    }

    public static RagDocument of(String content, Map<String, Object> metadata) {
        return of(null, content, metadata);
    }

    public static RagDocument of(String id, String content, Map<String, Object> metadata) {
        String documentId = id == null || id.isBlank() ? java.util.UUID.randomUUID().toString() : id.trim();
        return new RagDocument(documentId, content, metadata);
    }
}
