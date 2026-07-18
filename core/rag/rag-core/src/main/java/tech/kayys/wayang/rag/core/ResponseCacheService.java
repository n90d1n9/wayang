package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ResponseCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseCacheService.class);
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    public String get(String key) {
        CachedResponse cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            LOG.debug("Cache hit for key: {}", key);
            return cached.response();
        }
        return null;
    }

    public void put(String key, String response) {
        cache.put(key, new CachedResponse(
                response, Instant.now().plus(Duration.ofHours(1))));
    }

    public void clear() {
        cache.clear();
    }

    record CachedResponse(String response, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}