package tech.kayys.wayang.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSchemaRegistry {

    private static final Map<String, SchemaHandler> handlers = new ConcurrentHashMap<>();
    private static final JsonSchemaCache cache = new JsonSchemaCache();
    private static boolean cachingEnabled = true;

    static {
        initializeHandlers();
    }

    private static void initializeHandlers() {
        // Auto-register handlers will be called here
    }

    public static void register(String domainId, SchemaHandler handler) {
        if (domainId != null && !domainId.isBlank() && handler != null) {
            handlers.put(domainId, handler);
        }
    }

    public static Map<String, Object> getSchema(String domainId) {
        if (domainId == null || domainId.isBlank()) {
            return null;
        }

        if (cachingEnabled) {
            Map<String, Object> cached = cache.get(domainId);
            if (cached != null) {
                return cached;
            }
        }

        SchemaHandler handler = handlers.get(domainId);
        if (handler != null) {
            Map<String, Object> schema = handler.buildSchema();
            if (cachingEnabled && schema != null) {
                cache.put(domainId, schema);
            }
            return schema;
        }

        return null;
    }

    public static Map<String, Map<String, Object>> getSchemas() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        handlers.forEach((domainId, handler) -> {
            Map<String, Object> schema = getSchema(domainId);
            if (schema != null) {
                result.put(domainId, schema);
            }
        });
        return result;
    }

    public static boolean isRegistered(String domainId) {
        return domainId != null && handlers.containsKey(domainId);
    }

    public static void setCachingEnabled(boolean enabled) {
        cachingEnabled = enabled;
    }

    public static boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public static void clearCache() {
        cache.clear();
    }

    public static int getCacheSize() {
        return cache.size();
    }

    public static SchemaHandler getHandler(String domainId) {
        return handlers.get(domainId);
    }

    static Map<String, SchemaHandler> getHandlers() {
        return new LinkedHashMap<>(handlers);
    }
}
