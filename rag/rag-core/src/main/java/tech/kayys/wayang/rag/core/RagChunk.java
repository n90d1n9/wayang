package tech.kayys.wayang.rag.core;

import java.util.Map;
import java.util.UUID;

public record RagChunk(
        String id,
        String documentId,
        int chunkIndex,
        String text,
        Map<String, Object> metadata) {

    public static RagChunk of(String documentId, int chunkIndex, String text, Map<String, Object> metadata) {
        return new RagChunk(
                UUID.randomUUID().toString(),
                documentId,
                chunkIndex,
                text,
                metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
