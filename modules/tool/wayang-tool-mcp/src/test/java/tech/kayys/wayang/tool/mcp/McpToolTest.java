package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolTest {

    @Test
    void exposesDescriptorAndExecutesThroughClient() {
        McpToolClientTestDouble client = McpToolClientTestDouble.succeeding(McpToolCallResult.success(
                Map.of("text", "read file"),
                12));
        McpTool tool = new McpTool(
                "filesystem:read_file",
                "read_file",
                "Read a file",
                Map.of("type", "object"),
                client);

        ToolResult result = tool.executeAsync(Map.of("path", "/tmp/a.txt"), ToolContext.defaults())
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals("filesystem:read_file", tool.id());
        assertEquals(Map.of("type", "object"), tool.inputSchema());
        assertEquals(Map.of("path", "/tmp/a.txt"), client.lastInvocation().arguments());
        assertEquals(McpToolOutputFields.PROTOCOL, result.metadata().get(McpToolOutputFields.KEY_PROTOCOL));
    }

    @Test
    void surfacesClientFailureAsToolResultError() {
        McpTool tool = new McpTool(
                "filesystem:read_file",
                "read_file",
                "Read a file",
                Map.of(),
                McpToolClientTestDouble.succeeding(McpToolCallResult.failure(
                        "denied",
                        4,
                        McpFailureType.metadata(McpFailureType.JSON_RPC))));

        ToolResult result = tool.executeAsync(Map.of(), ToolContext.defaults())
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("denied", result.error());
        assertEquals(McpToolOutputFields.PROTOCOL, result.metadata().get(McpToolOutputFields.KEY_PROTOCOL));
        assertEquals(McpFailureType.JSON_RPC.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpFailureType.JSON_RPC.name(),
                ((Map<?, ?>) result.metadata().get(McpToolOutputFields.RAW_MCP_METADATA_KEY))
                        .get(McpFailureType.METADATA_KEY));
    }

    @Test
    void classifiesUnexpectedClientFailureAsTransport() {
        McpTool tool = new McpTool(
                "filesystem:read_file",
                "read_file",
                "Read a file",
                Map.of(),
                McpToolClientTestDouble.failing(new RuntimeException("socket closed")));

        ToolResult result = tool.executeAsync(Map.of(), ToolContext.defaults())
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("socket closed", result.error());
        assertEquals(McpToolOutputFields.PROTOCOL, result.metadata().get(McpToolOutputFields.KEY_PROTOCOL));
        assertEquals(McpFailureType.TRANSPORT.name(), result.metadata().get(McpFailureType.METADATA_KEY));
    }

    @Test
    void appliesDefaultExecutionContextAndAllowsInvocationOverrides() {
        McpToolClientTestDouble client = McpToolClientTestDouble.succeedingOk();
        McpTool tool = new McpTool(
                "docs:search",
                "search",
                "Search docs",
                Map.of(),
                Map.of(
                        HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, "http://default/mcp",
                        HttpMcpToolClient.CONTEXT_TOOL_NAME, "search"),
                client);

        ToolContext context = new ToolContext(
                "docs:search",
                Map.of(),
                java.nio.file.Path.of("."),
                Map.of(),
                Duration.ofSeconds(5),
                false,
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, "http://override/mcp"));

        tool.executeAsync(Map.of("q", "wayang"), context)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("http://override/mcp",
                client.lastInvocation().context().get(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT));
        assertEquals("search", client.lastInvocation().context().get(HttpMcpToolClient.CONTEXT_TOOL_NAME));
    }

    @Test
    void resultOutputCanRepresentNullPayloads() {
        Map<String, Object> output = McpToolCallResult.success(null, 1)
                .toOutput("filesystem:stat");

        assertTrue(output.containsKey(McpToolOutputFields.KEY_RESULT));
        assertEquals("", output.get(McpToolOutputFields.KEY_TEXT));
    }

}
