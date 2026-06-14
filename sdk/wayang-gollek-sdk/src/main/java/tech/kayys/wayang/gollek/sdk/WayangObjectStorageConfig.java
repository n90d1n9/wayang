package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record WayangObjectStorageConfig(
        String provider,
        String endpoint,
        String bucket,
        String region,
        String keyPrefix,
        boolean pathStyleAccess,
        String credentialsRef) {

    public WayangObjectStorageConfig {
        provider = SdkText.trimToDefault(provider, "s3").toLowerCase(Locale.ROOT);
        endpoint = SdkText.trimToEmpty(endpoint);
        bucket = SdkText.trimToEmpty(bucket);
        region = SdkText.trimToEmpty(region);
        keyPrefix = SdkText.trimToEmpty(keyPrefix);
        pathStyleAccess = pathStyleAccess || provider.equals("rustfs") || provider.equals("minio");
        credentialsRef = SdkText.trimToEmpty(credentialsRef);
    }

    public static WayangObjectStorageConfig none() {
        return new WayangObjectStorageConfig("", "", "", "", "", false, "");
    }

    public static WayangObjectStorageConfig s3(String endpoint, String bucket, String region, String keyPrefix) {
        return new WayangObjectStorageConfig("s3", endpoint, bucket, region, keyPrefix, false, "");
    }

    public static WayangObjectStorageConfig rustfs(String endpoint, String bucket, String keyPrefix) {
        return new WayangObjectStorageConfig("rustfs", endpoint, bucket, "", keyPrefix, true, "");
    }

    public static WayangObjectStorageConfig fromMap(Map<String, Object> values) {
        Map<String, Object> source = values == null ? Map.of() : values;
        return new WayangObjectStorageConfig(
                text(source, "provider"),
                text(source, "endpoint"),
                text(source, "bucket"),
                text(source, "region"),
                text(source, "keyPrefix", "objectKey", "key", "profileObjectKey", "readinessProfileObjectKey", "prefix"),
                bool(source, "pathStyleAccess", "pathStyle"),
                text(source, "credentialsRef", "credentials", "secretRef"));
    }

    public boolean configured() {
        return !bucket.isBlank() || !endpoint.isBlank() || !keyPrefix.isBlank();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("provider", provider);
        if (!endpoint.isBlank()) {
            values.put("endpoint", endpoint);
        }
        if (!bucket.isBlank()) {
            values.put("bucket", bucket);
        }
        if (!region.isBlank()) {
            values.put("region", region);
        }
        if (!keyPrefix.isBlank()) {
            values.put("keyPrefix", keyPrefix);
        }
        values.put("pathStyleAccess", pathStyleAccess);
        if (!credentialsRef.isBlank()) {
            values.put("credentialsRef", WayangSecretRedactor.connectionString(credentialsRef));
        }
        return SdkMaps.copy(values);
    }

    private static String text(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return SdkText.trimToEmpty(String.valueOf(value));
            }
        }
        return "";
    }

    private static boolean bool(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value != null) {
                return Boolean.parseBoolean(SdkText.trimToEmpty(String.valueOf(value)));
            }
        }
        return false;
    }
}
