package tech.kayys.golok.tools.spi;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * Base interface for all tools available in the golok platform.
 *
 * <p>
 * This SPI provides a unified interface for tools across different domains:
 * <ul>
 * <li>Code tools (file operations, search, execution)</li>
 * <li>Agent skills (inference, memory, RAG)</li>
 * <li>External tools (MCP servers, REST APIs, CLI commands)</li>
 * </ul>
 *
 * <p>
 * Each tool is a self-contained operation with:
 * <ul>
 * <li>A unique identifier and human-readable name</li>
 * <li>A description for LLM consumption</li>
 * <li>A JSON Schema defining input parameters</li>
 * <li>An execution method with structured results</li>
 * </ul>
 *
 * @author golok Team
 * @version 2.0.0
 */
public interface Tool {

    /**
     * Unique tool identifier (e.g., "read_file", "grep_search", "skill:rag").
     *
     * @return tool ID
     */
    String id();

    /**
     * Human-readable tool name.
     *
     * @return tool name
     */
    String name();

    /**
     * Description for the LLM to understand when to use this tool.
     * Should be clear and concise, highlighting the tool's purpose
     * and typical use cases.
     *
     * @return tool description
     */
    String description();

    /**
     * JSON Schema describing the tool's input parameters.
     * Follows JSON Schema Draft 7 format with:
     * <ul>
     * <li>{@code type}: "object"</li>
     * <li>{@code properties}: map of parameter schemas</li>
     * <li>{@code required}: list of required parameter names</li>
     * </ul>
     *
     * @return input schema
     */
    Map<String, Object> inputSchema();

    /**
     * Execute the tool with the given parameters.
     *
     * @param params  input parameters as key-value pairs
     * @param context execution context (working directory, environment, etc.)
     * @return tool execution result
     */
    ToolResult execute(Map<String, Object> params, ToolContext context);

    /**
     * Execute the tool reactively.
     * Default implementation wraps synchronous execution in a Uni.
     *
     * @param params  input parameters
     * @param context execution context
     * @return Uni containing the tool execution result
     */
    default Uni<ToolResult> executeAsync(Map<String, Object> params, ToolContext context) {
        return Uni.createFrom().item(execute(params, context));
    }
}
