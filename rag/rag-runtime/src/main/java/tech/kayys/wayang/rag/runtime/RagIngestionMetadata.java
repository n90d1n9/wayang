package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class RagIngestionMetadata {

    private RagIngestionMetadata() {
    }

    static Map<String, Object> pdf(String tenantId, Path path, Map<String, String> metadata) {
        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        put(documentMetadata, RagMetadataKeys.TENANT_ID, tenantId);
        documentMetadata.put(RagMetadataKeys.SOURCE, fileName(path));
        copyStringMetadata(metadata, documentMetadata);
        documentMetadata.put(
                RagRuntimeDefaults.COLLECTION_METADATA_KEY,
                RagRuntimeDefaults.normalizeCollection(value(
                        metadata,
                        RagRuntimeDefaults.COLLECTION_METADATA_KEY,
                        null)));
        return copy(documentMetadata);
    }

    static Map<String, Object> text(String tenantId, Map<String, String> metadata) {
        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        put(documentMetadata, RagMetadataKeys.TENANT_ID, tenantId);
        copyStringMetadata(metadata, documentMetadata);
        return copy(documentMetadata);
    }

    static Map<String, Object> copy(Map<String, Object> metadata) {
        return RagRuntimeMetadata.copy(metadata);
    }

    static String source(Map<String, Object> metadata) {
        return value(metadata, RagMetadataKeys.SOURCE, "");
    }

    static String value(Map<?, ?> metadata, String key, String defaultValue) {
        if (metadata == null) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        if (value == null || value.toString().isBlank()) {
            return defaultValue;
        }
        return value.toString();
    }

    private static void copyStringMetadata(Map<String, String> source, Map<String, Object> target) {
        RagRuntimeMetadata.copyStringsInto(source, target);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (key != null && value != null) {
            target.put(key, value);
        }
    }

    private static String fileName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return path.getFileName().toString();
    }
}
