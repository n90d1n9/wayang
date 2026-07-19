package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.util.Locale;

final class McpServerTransports {

    static final String HTTP = "http";
    static final String HTTP_SSE = "http+sse";
    static final String SSE = "sse";
    static final String STDIO = "stdio";
    static final String STREAMABLE_HTTP = "streamable-http";

    private McpServerTransports() {
    }

    static String normalize(String transport) {
        return transport == null ? "" : transport.trim().toLowerCase(Locale.ROOT);
    }

    static boolean matches(String actual, String expected) {
        String normalizedExpected = normalize(expected);
        return normalizedExpected.isBlank() || normalize(actual).equals(normalizedExpected);
    }

    static boolean isHttp(String transport) {
        return HTTP.equals(normalize(transport));
    }

    static boolean supportsHttpDiscovery(McpServerRegistry server) {
        return server != null && supportsHttpDiscovery(server.getTransport());
    }

    static boolean supportsHttpDiscovery(String transport) {
        String normalized = normalize(transport);
        return normalized.isBlank()
                || HTTP.equals(normalized)
                || SSE.equals(normalized)
                || STREAMABLE_HTTP.equals(normalized)
                || HTTP_SSE.equals(normalized);
    }
}
