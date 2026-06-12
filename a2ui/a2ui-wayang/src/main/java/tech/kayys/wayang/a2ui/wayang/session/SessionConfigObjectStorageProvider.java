package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-free provider adapter for bucket/key session config storage.
 */
public final class SessionConfigObjectStorageProvider implements SessionConfigSourceProvider {

    public static final String TYPE_S3 = "s3";
    public static final String TYPE_RUSTFS = "rustfs";
    public static final String KEY_BUCKET = "bucket";
    public static final String KEY_KEY = "key";

    private final String type;
    private final ObjectReader reader;

    public SessionConfigObjectStorageProvider(String type, ObjectReader reader) {
        this.type = requireText(type, "type");
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public static SessionConfigObjectStorageProvider s3(ObjectReader reader) {
        return new SessionConfigObjectStorageProvider(TYPE_S3, reader);
    }

    public static SessionConfigObjectStorageProvider rustfs(ObjectReader reader) {
        return new SessionConfigObjectStorageProvider(TYPE_RUSTFS, reader);
    }

    @Override
    public SessionConfigSource source(Map<String, Object> values) {
        requireValid(values);
        String bucket = bucket(values);
        String key = key(values);
        return SessionConfigSources.json(description(bucket, key), () -> {
            Optional<String> json = reader.read(bucket, key, Map.copyOf(values));
            return json == null ? Optional.empty() : json;
        });
    }

    @Override
    public List<String> validationErrors(Map<String, Object> values) {
        List<String> errors = new ArrayList<>();
        if (bucket(values).isBlank()) {
            errors.add(type + " source requires bucket");
        }
        if (key(values).isBlank()) {
            errors.add(type + " source requires key");
        }
        return List.copyOf(errors);
    }

    @Override
    public SessionConfigSourceCapability capability(String type) {
        return SessionConfigSourceCapability.objectStorage(type);
    }

    private void requireValid(Map<String, Object> values) {
        List<String> errors = validationErrors(values);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private String description(String bucket, String key) {
        return type + ":" + bucket + "/" + key;
    }

    private static String bucket(Map<String, Object> values) {
        return text(values, KEY_BUCKET, "container");
    }

    private static String key(Map<String, Object> values) {
        return text(values, KEY_KEY, "objectKey", "path");
    }

    private static String text(Map<String, Object> values, String... keys) {
        if (values == null) {
            return "";
        }
        for (String key : keys) {
            String value = DecodeValues.text(values.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface ObjectReader {

        Optional<String> read(String bucket, String key, Map<String, Object> values);
    }
}
