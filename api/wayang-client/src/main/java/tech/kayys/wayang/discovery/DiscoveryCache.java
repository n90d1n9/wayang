package tech.kayys.wayang.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Simple cache for discovery results.
 * Memoizes expensive discovery operations.
 */
public class DiscoveryCache<T> {

    private final Map<String, Object> cache = new HashMap<>();

    /**
     * Get value from cache or compute and cache it.
     */
    @SuppressWarnings("unchecked")
    public T getOrCompute(String key, Supplier<T> supplier) {
        if (cache.containsKey(key)) {
            return (T) cache.get(key);
        }
        T value = supplier.get();
        cache.put(key, value);
        return value;
    }

    /**
     * Clear all cached values.
     */
    public void clear() {
        cache.clear();
    }
}
