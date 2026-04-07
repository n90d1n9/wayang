package tech.kayys.gollek.agent.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

/**
 * SPI for connecting to a Model Context Protocol (MCP) server.
 *
 * <p>Each implementation handles a specific transport:
 * <ul>
 *   <li>{@code McpSseClient} — HTTP + Server-Sent Events (remote servers)</li>
 *   <li>{@code McpStdioClient} — stdio (local process spawning)</li>
 * </ul>
 *
 * <p>The client is responsible for the full MCP lifecycle:
 * initialize → tools/list → tools/call → (optionally) resources/list, etc.
 */
public interface McpClient extends AutoCloseable {

    /** Unique identifier for this client / server connection. */
    String serverId();

    /** Display name of the connected MCP server. */
    String serverName();

    /** Whether this client is connected and initialised. */
    boolean isConnected();

    /**
     * Connect to the MCP server and perform the initialize handshake.
     * Must be called before any other operations.
     */
    Uni<Void> connect();

    /**
     * Disconnect and release all resources.
     */
    Uni<Void> disconnect();

    /**
     * List all tools available on this MCP server.
     */
    Uni<List<McpProtocol.McpTool>> listTools();

    /**
     * Call a tool on the MCP server.
     *
     * @param toolName  name of the tool
     * @param arguments tool arguments (must match the tool's inputSchema)
     * @return tool result content
     */
    Uni<McpProtocol.CallToolResult> callTool(String toolName, Map<String, Object> arguments);

    /**
     * List resources available on this MCP server (if supported).
     */
    Uni<List<McpProtocol.McpResource>> listResources();

    /**
     * Read a specific resource by URI.
     */
    Uni<McpProtocol.ReadResourceResult> readResource(String uri);

    /**
     * List prompts available on this MCP server (if supported).
     */
    Uni<List<McpProtocol.McpPrompt>> listPrompts();

    /**
     * Get a prompt with resolved arguments.
     */
    Uni<McpProtocol.GetPromptResult> getPrompt(String name, Map<String, String> arguments);

    /**
     * Get the server capabilities reported during initialization.
     * Returns null if not yet connected.
     */
    McpProtocol.ServerCapabilities capabilities();

    @Override
    default void close() {
        disconnect().subscribe().with(v -> {}, e -> {});
    }
}
