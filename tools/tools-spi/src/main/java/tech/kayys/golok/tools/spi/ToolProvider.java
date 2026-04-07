package tech.kayys.golok.tools.spi;

import java.util.List;
import java.util.Map;

/**
 * SPI for external tool providers.
 *
 * <p>
 * Tool providers enable agents to interact with external systems:
 * <ul>
 * <li><b>MCP Servers</b> - Model Context Protocol servers</li>
 * <li><b>REST APIs</b> - HTTP APIs with OpenAPI specs</li>
 * <li><b>CLI Commands</b> - Shell commands with structured I/O</li>
 * <li><b>gRPC Services</b> - Protocol buffer-based services</li>
 * <li><b>Database Connections</b> - SQL/NoSQL query interfaces</li>
 * </ul>
 *
 * <h3>Tool Discovery:</h3>
 * Providers are discovered via ServiceLoader or dynamic registration.
 * Each provider can expose multiple tools.
 *
 * <h3>Tool Execution:</h3>
 * Tools are executed with typed inputs and return structured results.
 *
 * <h3>Usage:</h3>
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class MyToolProvider implements ToolProvider {
 *         &#64;Override
 *         public ToolSource source() {
 *             return ToolSource.REST_API;
 *         }
 *
 *         &#64;Override
 *         public List<ToolDescriptor> discoverTools() {
 *             // Return available tools
 *         }
 *
 *         @Override
 *         public ToolResult execute(ToolContext context) {
 *             // Execute the tool
 *         }
 *     }
 * }
 * </pre>
 *
 * @author golok Team
 * @version 2.0.0
 */
public interface ToolProvider {

    /**
     * Tool source type identifier.
     *
     * @return source type (e.g., "mcp", "rest", "cli")
     */
    ToolSource source();

    /**
     * Discover available tools from this provider.
     *
     * @return list of tool descriptors
     */
    List<ToolDescriptor> discoverTools();

    /**
     * Execute a tool.
     *
     * @param context tool execution context
     * @return tool execution result
     */
    ToolResult execute(ToolContext context);

    /**
     * Check if this provider is available.
     *
     * @return true if provider is connected and ready
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Get provider-specific metadata.
     *
     * @return metadata map
     */
    default Map<String, Object> getMetadata() {
        return Map.of();
    }

    /**
     * Get the provider ID.
     *
     * @return provider identifier
     */
    default String getProviderId() {
        return source().id();
    }
}
