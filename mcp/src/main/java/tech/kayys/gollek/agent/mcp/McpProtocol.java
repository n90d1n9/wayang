package tech.kayys.gollek.agent.mcp;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Protocol types for the Model Context Protocol (MCP) 2024-11 specification.
 *
 * <p>MCP is a standard for connecting AI assistants to external data sources
 * and tools. It uses JSON-RPC 2.0 over SSE (Server-Sent Events) or stdio.
 * See: <a href="https://modelcontextprotocol.io/specification">MCP Spec</a></p>
 *
 * <h2>Message flow</h2>
 * <pre>
 * Client → Server: initialize request
 * Server → Client: initialize response (capabilities)
 * Client → Server: initialized notification
 * Client → Server: tools/list request
 * Server → Client: tools/list response
 * Client → Server: tools/call request
 * Server → Client: tools/call response
 * </pre>
 */
public final class McpProtocol {

    private McpProtocol() {}

    public static final String JSONRPC_VERSION = "2.0";
    public static final String MCP_VERSION     = "2024-11-05";

    // ── JSON-RPC base types ────────────────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonRpcRequest(
            String jsonrpc,
            String id,
            String method,
            Object params
    ) {
        public JsonRpcRequest(String id, String method, Object params) {
            this(JSONRPC_VERSION, id, method, params);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonRpcNotification(
            String jsonrpc,
            String method,
            Object params
    ) {
        public JsonRpcNotification(String method, Object params) {
            this(JSONRPC_VERSION, method, params);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonRpcResponse(
            String jsonrpc,
            String id,
            JsonNode result,
            JsonRpcError error
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonRpcError(int code, String message, Object data) {}

    // ── Initialize ────────────────────────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InitializeParams(
            String protocolVersion,
            ClientCapabilities capabilities,
            ClientInfo clientInfo
    ) {
        public static InitializeParams of(String clientName, String clientVersion) {
            return new InitializeParams(
                    MCP_VERSION,
                    new ClientCapabilities(new RootsCapability(false), new SamplingCapability()),
                    new ClientInfo(clientName, clientVersion)
            );
        }
    }

    public record ClientInfo(String name, String version) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientCapabilities(RootsCapability roots, SamplingCapability sampling) {}

    public record RootsCapability(boolean listChanged) {}
    public record SamplingCapability() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeResult(
            String protocolVersion,
            ServerCapabilities capabilities,
            ServerInfo serverInfo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerCapabilities(
            ToolsCapability tools,
            ResourcesCapability resources,
            PromptsCapability prompts
    ) {
        public boolean hasTools()     { return tools     != null; }
        public boolean hasResources() { return resources != null; }
        public boolean hasPrompts()   { return prompts   != null; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolsCapability(boolean listChanged) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourcesCapability(boolean subscribe, boolean listChanged) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptsCapability(boolean listChanged) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerInfo(String name, String version) {}

    // ── Tools ─────────────────────────────────────────────────────────────────

    public record ListToolsParams(String cursor) {
        public ListToolsParams() { this(null); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListToolsResult(List<McpTool> tools, String nextCursor) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record McpTool(
            String name,
            String description,
            InputSchema inputSchema
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InputSchema(
            String type,
            Map<String, Object> properties,
            List<String> required
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CallToolParams(String name, Map<String, Object> arguments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolResult(List<ContentBlock> content, Boolean isError) {
        /** Extract the first text content block's text value. */
        public String firstText() {
            if (content == null || content.isEmpty()) return "";
            return content.stream()
                    .filter(c -> "text".equals(c.type()))
                    .map(ContentBlock::text)
                    .findFirst()
                    .orElse("");
        }
        /** Combines all text content blocks. */
        public String allText() {
            if (content == null) return "";
            return content.stream()
                    .filter(c -> "text".equals(c.type()) && c.text() != null)
                    .map(ContentBlock::text)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
        }
        public boolean failed() { return Boolean.TRUE.equals(isError); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(
            String type,       // "text" | "image" | "resource"
            String text,       // for type="text"
            String mimeType,   // for type="image" or "resource"
            String data,       // base64 for type="image"
            ResourceReference resource  // for type="resource"
    ) {}

    // ── Resources ─────────────────────────────────────────────────────────────

    public record ListResourcesParams(String cursor) {
        public ListResourcesParams() { this(null); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListResourcesResult(List<McpResource> resources, String nextCursor) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record McpResource(String uri, String name, String description, String mimeType) {}

    public record ReadResourceParams(String uri) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReadResourceResult(List<ContentBlock> contents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourceReference(String uri, String mimeType) {}

    // ── Prompts ───────────────────────────────────────────────────────────────

    public record ListPromptsParams(String cursor) {
        public ListPromptsParams() { this(null); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListPromptsResult(List<McpPrompt> prompts, String nextCursor) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record McpPrompt(String name, String description, List<PromptArgument> arguments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptArgument(String name, String description, boolean required) {}

    public record GetPromptParams(String name, Map<String, String> arguments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GetPromptResult(String description, List<PromptMessage> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptMessage(String role, ContentBlock content) {}

    // ── Notifications ─────────────────────────────────────────────────────────

    public record InitializedNotification() {}

    // ── Error codes (JSON-RPC + MCP-specific) ────────────────────────────────

    public static final int ERR_PARSE_ERROR       = -32700;
    public static final int ERR_INVALID_REQUEST   = -32600;
    public static final int ERR_METHOD_NOT_FOUND  = -32601;
    public static final int ERR_INVALID_PARAMS    = -32602;
    public static final int ERR_INTERNAL          = -32603;
    public static final int ERR_NOT_FOUND         = -32002; // MCP: resource/tool not found
    public static final int ERR_INVALID_CURSOR    = -32001; // MCP: invalid pagination cursor
}
