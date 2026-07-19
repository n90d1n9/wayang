package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHttpTransportContextTest {

    @Test
    void endpointReadsDirectAliasesBeforeCustomData() {
        Map<String, Object> context = Map.of(
                McpHttpTransportContext.CONTEXT_URL, "http://direct.local/mcp",
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_MCP_ENDPOINT, "http://custom.local/mcp"));

        assertEquals("http://direct.local/mcp", McpHttpTransportContext.endpoint(context).orElseThrow());
    }

    @Test
    void endpointFallsBackToCustomDataAliases() {
        Map<String, Object> context = Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_SERVER_URL, "http://custom.local/mcp"));

        assertEquals("http://custom.local/mcp", McpHttpTransportContext.endpoint(context).orElseThrow());
    }

    @Test
    void protocolVersionReadsCustomDataAndDefaultsWhenMissing() {
        assertEquals(McpHttpTransportContext.DEFAULT_PROTOCOL_VERSION,
                McpHttpTransportContext.protocolVersion(Map.of()));
        assertEquals("2026-01-01", McpHttpTransportContext.protocolVersion(Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_PROTOCOL_VERSION, "2026-01-01"))));
    }

    @Test
    void headersNormalizeValuesAndReadCustomData() {
        Map<String, Object> context = Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_MCP_HEADERS, Map.of(
                                "Authorization", "Bearer token",
                                "Retry", 2)));

        assertEquals(Map.of(
                "Authorization", "Bearer token",
                "Retry", "2"), McpHttpTransportContext.headers(context));
    }

    @Test
    void timeoutReadsDirectAndCustomContextValues() {
        assertEquals(Duration.ofMillis(1200), McpHttpTransportContext.timeout(Map.of(
                McpHttpTransportContext.CONTEXT_TIMEOUT_MS, 1200)));
        assertEquals(Duration.ofMillis(1500), McpHttpTransportContext.timeout(Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_TIMEOUT_MS, "1500"))));
        assertEquals(1500, McpHttpTransportContext.timeoutMs(Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of(
                        McpHttpTransportContext.CONTEXT_TIMEOUT_MS, "1500"))));
    }

    @Test
    void invalidTimeoutValuesFallBackToDefault() {
        assertEquals(Duration.ofSeconds(30), McpHttpTransportContext.timeout(Map.of(
                McpHttpTransportContext.CONTEXT_TIMEOUT_MS, "slow")));
        assertEquals(30000, McpHttpTransportContext.timeoutMs(Map.of(
                McpHttpTransportContext.CONTEXT_TIMEOUT_MS, 0)));
        assertEquals(Duration.ofSeconds(30), McpHttpTransportContext.timeout(null));
        assertTrue(McpHttpTransportContext.endpoint(null).isEmpty());
    }
}
