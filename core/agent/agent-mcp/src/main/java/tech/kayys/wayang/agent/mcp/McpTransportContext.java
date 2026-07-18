package tech.kayys.wayang.agent.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral context keys attached to MCP tool invocations.
 */
public final class McpTransportContext {

    public static final String KEY_ARGS = "args";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_HEADERS = "headers";
    public static final String KEY_MCP_HEADERS = "mcpHeaders";
    public static final String KEY_MCP_ENDPOINT = "mcpEndpoint";
    public static final String KEY_PROTOCOL_VERSION = "mcpProtocolVersion";
    public static final String KEY_SERVER_ID = "serverId";
    public static final String KEY_TIMEOUT_MS = "timeoutMs";
    public static final String KEY_TRANSPORT_TYPE = "transportType";
    public static final String KEY_URL = "url";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-11-25";

    private McpTransportContext() {
    }

    public static Map<String, Object> fromServer(McpServerConfig server) {
        Objects.requireNonNull(server, "server");
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(KEY_SERVER_ID, server.id());
        context.put(KEY_TRANSPORT_TYPE, server.transportType().name());
        putIfPresent(context, KEY_MCP_ENDPOINT, server.url());
        putIfPresent(context, KEY_URL, server.url());
        putIfPresent(context, KEY_COMMAND, server.command());
        if (!server.args().isEmpty()) {
            context.put(KEY_ARGS, server.args());
        }
        if (!server.headers().isEmpty()) {
            context.put(KEY_HEADERS, server.headers());
        }
        return McpMaps.copy(context);
    }

    public static Map<String, Object> merge(McpServerConfig server, Map<String, Object> metadata) {
        Map<String, Object> merged = new LinkedHashMap<>(McpMaps.copy(metadata));
        merged.putAll(fromServer(server));
        return McpMaps.copy(merged);
    }

    private static void putIfPresent(Map<String, Object> context, String key, String value) {
        if (value != null && !value.isBlank()) {
            context.put(key, value.trim());
        }
    }
}
