package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provider-neutral object storage location for Agentic Commerce persistence.
 */
public record AgenticCommerceObjectStoreConfig(
        String provider,
        String endpoint,
        String region,
        String bucket,
        String keyPrefix,
        Map<String, Object> attributes) {

    public static final String PROVIDER_OBJECT_STORE = "object-store";
    public static final String PROVIDER_S3 = "s3";
    public static final String PROVIDER_RUSTFS = "rustfs";

    public AgenticCommerceObjectStoreConfig {
        provider = normalizeProvider(provider);
        endpoint = normalizeEndpoint(endpoint);
        region = AgenticCommerceWayangMaps.text(region);
        bucket = AgenticCommerceWayangMaps.required(bucket, "object store bucket");
        keyPrefix = normalizeKeyPrefix(keyPrefix);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceObjectStoreConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Map<String, Object> nested = firstMap(
                resolved,
                "objectStore",
                "objectStorage",
                "s3",
                "rustfs",
                "cloudStorage");
        Map<String, Object> merged = new LinkedHashMap<>(resolved);
        merged.putAll(nested);
        String provider = AgenticCommerceWayangMaps.firstText(
                merged,
                "provider",
                "storageProvider",
                "storageKind",
                "kind",
                "type");
        String nestedProvider = nestedProvider(resolved);
        if (!nestedProvider.isBlank() && PROVIDER_OBJECT_STORE.equals(normalizeProvider(provider))) {
            provider = nestedProvider;
        }
        return new AgenticCommerceObjectStoreConfig(
                provider,
                AgenticCommerceWayangMaps.firstText(merged, "endpoint", "endpointUrl", "url"),
                AgenticCommerceWayangMaps.firstText(merged, "region", "awsRegion"),
                AgenticCommerceWayangMaps.firstText(merged, "bucket", "bucketName"),
                AgenticCommerceWayangMaps.firstText(merged, "keyPrefix", "prefix", "root", "directory"),
                firstMap(
                        merged,
                        "attributes",
                        "metadata",
                        "clientAttributes"));
    }

    public String objectKey(String name) {
        String normalizedName = AgenticCommerceWayangMaps.required(name, "object name");
        return keyPrefix.isBlank() ? normalizedName : keyPrefix + "/" + normalizedName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("provider", provider);
        values.put("endpoint", endpoint);
        values.put("endpointConfigured", !endpoint.isBlank());
        values.put("region", region);
        values.put("bucket", bucket);
        values.put("keyPrefix", keyPrefix);
        values.put("attributeCount", attributes.size());
        return Map.copyOf(values);
    }

    public Map<String, Object> toStorageMap() {
        Map<String, Object> values = new LinkedHashMap<>(toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static String normalizeProvider(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()
                || "cloud".equals(normalized)
                || "cloud-storage".equals(normalized)
                || "object-storage".equals(normalized)
                || "objectstore".equals(normalized)) {
            return PROVIDER_OBJECT_STORE;
        }
        if ("aws-s3".equals(normalized) || "s3-compatible".equals(normalized) || "minio".equals(normalized)) {
            return PROVIDER_S3;
        }
        if ("rust-fs".equals(normalized) || "rust_fs".equals(normalized)) {
            return PROVIDER_RUSTFS;
        }
        return normalized;
    }

    private static Map<String, Object> firstMap(Map<String, Object> values, String... keys) {
        Object value = AgenticCommerceWayangMaps.first(values, keys);
        return value instanceof Map<?, ?> map ? AgenticCommerceWayangMaps.copy(map) : Map.of();
    }

    private static String nestedProvider(Map<String, Object> values) {
        if (values.containsKey("s3")) {
            return PROVIDER_S3;
        }
        if (values.containsKey("rustfs")) {
            return PROVIDER_RUSTFS;
        }
        return "";
    }

    private static String normalizeEndpoint(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeKeyPrefix(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
