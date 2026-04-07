package tech.kayys.wayang.embedding;

import java.util.LinkedHashMap;
import java.util.Map;

final class EmbeddingVectorCache {

    private final int maxEntries;
    private final Map<String, float[]> lru;

    EmbeddingVectorCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        this.maxEntries = maxEntries;
        this.lru = new LinkedHashMap<>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                return size() > EmbeddingVectorCache.this.maxEntries;
            }
        };
    }

    float[] get(String key) {
        synchronized (lru) {
            return lru.get(key);
        }
    }

    void put(String key, float[] value) {
        synchronized (lru) {
            lru.put(key, value);
        }
    }

    void clear() {
        synchronized (lru) {
            lru.clear();
        }
    }
}
