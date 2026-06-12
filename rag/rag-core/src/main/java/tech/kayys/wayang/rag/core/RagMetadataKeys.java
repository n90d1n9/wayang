package tech.kayys.wayang.rag.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RagMetadataKeys {

    public static final String TENANT_ID = "tenantId";
    public static final String SOURCE = "source";
    public static final String COLLECTION = "collection";
    public static final String EMBEDDING_MODEL = "embeddingModel";
    public static final String EMBEDDING_DIMENSION = "embeddingDimension";
    public static final String EMBEDDING_VERSION = "embeddingVersion";
    public static final String DOCUMENT_ID = "documentId";
    public static final String CHUNK_INDEX = "chunkIndex";

    private RagMetadataKeys() {
    }

    public static Map<String, Object> embeddingScope(
            String tenantId,
            String embeddingModel,
            int embeddingDimension,
            String embeddingVersion) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, TENANT_ID, tenantId);
        putIfPresent(metadata, EMBEDDING_MODEL, embeddingModel);
        metadata.put(EMBEDDING_DIMENSION, embeddingDimension);
        putIfPresent(metadata, EMBEDDING_VERSION, embeddingVersion);
        return RagMetadata.copy(metadata);
    }

    public static Map<String, Object> indexedChunk(
            String tenantId,
            String embeddingModel,
            int embeddingDimension,
            String embeddingVersion,
            RagChunk chunk) {

        Objects.requireNonNull(chunk, "chunk");
        Map<String, Object> metadata = new LinkedHashMap<>(embeddingScope(
                tenantId,
                embeddingModel,
                embeddingDimension,
                embeddingVersion));
        putIfPresent(metadata, DOCUMENT_ID, chunk.documentId());
        metadata.put(CHUNK_INDEX, chunk.chunkIndex());
        return RagMetadata.copy(metadata);
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
