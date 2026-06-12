package tech.kayys.wayang.a2ui.wayang.session;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory helpers for common provider-backed A2UI session config source specs.
 */
public final class SessionConfigSourceSpecs {

    public static SessionConfigSourceSpec database(String tenantId) {
        return database(tenantId, "");
    }

    public static SessionConfigSourceSpec database(String tenantId, String profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigLookupProvider.KEY_TENANT_ID, requireText(tenantId, "tenantId"));
        putIfPresent(values, "profile", profile);
        return SessionConfigSourceSpec.provider(SessionConfigLookupProvider.TYPE_DATABASE, values);
    }

    public static SessionConfigSourceSpec configService(String key) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigLookupProvider.KEY_LOOKUP_KEY, requireText(key, "key"));
        return SessionConfigSourceSpec.provider(SessionConfigLookupProvider.TYPE_CONFIG_SERVICE, values);
    }

    public static SessionConfigSourceSpec s3(String bucket, String key) {
        return objectStorage(SessionConfigObjectStorageProvider.TYPE_S3, bucket, key);
    }

    public static SessionConfigSourceSpec rustfs(String bucket, String key) {
        return objectStorage(SessionConfigObjectStorageProvider.TYPE_RUSTFS, bucket, key);
    }

    public static SessionConfigSourceSpec objectStorage(String type, String bucket, String key) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(SessionConfigObjectStorageProvider.KEY_BUCKET, requireText(bucket, "bucket"));
        values.put(SessionConfigObjectStorageProvider.KEY_KEY, requireText(key, "key"));
        return SessionConfigSourceSpec.provider(requireText(type, "type"), values);
    }

    public static SessionConfigSourceSpec lookup(String type, String tenantId, String key) {
        Map<String, Object> values = new LinkedHashMap<>();
        putIfPresent(values, SessionConfigLookupProvider.KEY_TENANT_ID, tenantId);
        putIfPresent(values, SessionConfigLookupProvider.KEY_LOOKUP_KEY, key);
        return SessionConfigSourceSpec.provider(requireText(type, "type"), values);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value.trim());
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    private SessionConfigSourceSpecs() {
    }
}
