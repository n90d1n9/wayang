package tech.kayys.wayang.tool.mcp;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class McpHttpTransportContext {

    static final String CONTEXT_MCP_ENDPOINT = "mcpEndpoint";
    static final String CONTEXT_MCP_URL = "mcpUrl";
    static final String CONTEXT_SERVER_URL = "serverUrl";
    static final String CONTEXT_URL = "url";
    static final String CONTEXT_HEADERS = "headers";
    static final String CONTEXT_MCP_HEADERS = "mcpHeaders";
    static final String CONTEXT_TIMEOUT_MS = "timeoutMs";
    static final String CONTEXT_PROTOCOL_VERSION = "mcpProtocolVersion";
    static final String DEFAULT_PROTOCOL_VERSION = "2025-11-25";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private McpHttpTransportContext() {
    }

    static String protocolVersion(Map<String, Object> context) {
        return string(context, CONTEXT_PROTOCOL_VERSION)
                .orElse(DEFAULT_PROTOCOL_VERSION);
    }

    static Optional<String> endpoint(Map<String, Object> context) {
        return string(context, CONTEXT_MCP_ENDPOINT, CONTEXT_MCP_URL, CONTEXT_SERVER_URL, CONTEXT_URL);
    }

    static Map<String, String> headers(Map<String, Object> context) {
        Map<String, Object> rawHeaders = directMap(context, CONTEXT_MCP_HEADERS)
                .or(() -> directMap(context, CONTEXT_HEADERS))
                .or(() -> customMap(context, CONTEXT_MCP_HEADERS))
                .or(() -> customMap(context, CONTEXT_HEADERS))
                .orElse(Map.of());
        Map<String, String> headers = new LinkedHashMap<>();
        rawHeaders.forEach((key, value) -> {
            if (value != null) {
                headers.put(key, String.valueOf(value));
            }
        });
        return Map.copyOf(headers);
    }

    static Duration timeout(Map<String, Object> context) {
        return value(context, CONTEXT_TIMEOUT_MS)
                .flatMap(McpHttpTransportContext::timeoutFromValue)
                .orElse(DEFAULT_TIMEOUT);
    }

    static int timeoutMs(Map<String, Object> context) {
        long millis = timeout(context).toMillis();
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }

    private static Optional<String> string(Map<String, Object> context, String... keys) {
        for (String key : keys) {
            Optional<String> value = directString(context, key);
            if (value.isPresent()) {
                return value;
            }
        }
        for (String key : keys) {
            Optional<String> value = customString(context, key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> value(Map<String, Object> context, String key) {
        return directValue(context, key)
                .or(() -> directValue(McpToolInvocationFields.customData(context), key));
    }

    private static Optional<Object> directValue(Map<String, Object> values, String key) {
        if (values == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(key));
    }

    private static Optional<String> directString(Map<String, Object> values, String key) {
        return directValue(values, key)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank());
    }

    private static Optional<String> customString(Map<String, Object> context, String key) {
        return directString(McpToolInvocationFields.customData(context), key);
    }

    private static Optional<Map<String, Object>> directMap(Map<String, Object> values, String key) {
        if (values == null) {
            return Optional.empty();
        }
        return Optional.of(McpMaps.fromObject(values.get(key)))
                .filter(map -> !map.isEmpty());
    }

    private static Optional<Map<String, Object>> customMap(Map<String, Object> context, String key) {
        return directMap(McpToolInvocationFields.customData(context), key);
    }

    private static Optional<Duration> timeoutFromValue(Object value) {
        if (value instanceof Number number && number.longValue() > 0) {
            return Optional.of(Duration.ofMillis(number.longValue()));
        }
        if (value instanceof String string) {
            try {
                long millis = Long.parseLong(string);
                if (millis > 0) {
                    return Optional.of(Duration.ofMillis(millis));
                }
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
