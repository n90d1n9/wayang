package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Minimal S3/RustFS-compatible object client boundary for persistence adapters.
 */
public interface AgenticCommerceObjectStoreClient {

    Optional<String> readText(String bucket, String key);

    void writeText(String bucket, String key, String contentType, String body);

    Map<String, Object> toMap();
}
