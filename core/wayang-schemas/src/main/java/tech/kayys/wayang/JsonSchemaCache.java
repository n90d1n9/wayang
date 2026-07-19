package tech.kayys.wayang.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSchemaCache {

    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public void put(String key, Map<String, Object> schema) {
        if (key != null && schema != null) {
            cache.put(key, new LinkedHashMap<>(schema));
        }
    }

    public Map<String, Object> get(String key) {
        Map<String, Object> cached = cache.get(key);
        if (cached != null) {
            return new LinkedHashMap<>(cached);
        }
        return null;
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }
}
