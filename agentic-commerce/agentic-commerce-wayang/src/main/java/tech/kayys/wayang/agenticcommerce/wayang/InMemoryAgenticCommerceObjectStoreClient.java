package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic object-store client for tests and local embedded persistence.
 */
public final class InMemoryAgenticCommerceObjectStoreClient implements AgenticCommerceObjectStoreClient {

    private final Map<String, StoredObject> objects = new LinkedHashMap<>();

    public static InMemoryAgenticCommerceObjectStoreClient create() {
        return new InMemoryAgenticCommerceObjectStoreClient();
    }

    public boolean contains(String bucket, String key) {
        synchronized (objects) {
            return objects.containsKey(objectId(bucket, key));
        }
    }

    public Map<String, String> snapshot() {
        synchronized (objects) {
            Map<String, String> values = new LinkedHashMap<>();
            objects.forEach((key, value) -> values.put(key, value.body()));
            return Map.copyOf(values);
        }
    }

    @Override
    public Optional<String> readText(String bucket, String key) {
        synchronized (objects) {
            StoredObject object = objects.get(objectId(bucket, key));
            return object == null ? Optional.empty() : Optional.of(object.body());
        }
    }

    @Override
    public void writeText(String bucket, String key, String contentType, String body) {
        synchronized (objects) {
            objects.put(
                    objectId(bucket, key),
                    new StoredObject(
                            AgenticCommerceWayangMaps.text(contentType),
                            body == null ? "" : body));
        }
    }

    @Override
    public Map<String, Object> toMap() {
        synchronized (objects) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("clientKind", "in-memory");
            values.put("objectCount", objects.size());
            values.put("bucketCount", objects.keySet().stream()
                    .map(key -> key.substring(0, key.indexOf('/')))
                    .distinct()
                    .count());
            return Map.copyOf(values);
        }
    }

    private static String objectId(String bucket, String key) {
        return AgenticCommerceWayangMaps.required(bucket, "object store bucket")
                + "/"
                + AgenticCommerceWayangMaps.required(key, "object store key");
    }

    private record StoredObject(String contentType, String body) {
    }
}
