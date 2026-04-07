package tech.kayys.golok.tools.spi;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for tool discovery and management.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Tool registration and lookup</li>
 * <li>Tool grouping by category</li>
 * <li>Enable/disable functionality</li>
 * <li>Schema generation for LLM consumption</li>
 * <li>Tool execution with caching</li>
 * </ul>
 *
 * @author golok Team
 * @version 2.0.0
 */
public interface ToolRegistry {

    /**
     * Register a tool in the registry.
     *
     * @param tool the tool to register
     */
    void register(Tool tool);

    /**
     * Register a tool with a category group.
     *
     * @param tool  the tool to register
     * @param group the category group
     */
    void register(Tool tool, String group);

    /**
     * Get a tool by ID.
     *
     * @param toolId the tool identifier
     * @return optional tool
     */
    Optional<Tool> get(String toolId);

    /**
     * Check if a tool exists.
     *
     * @param toolId the tool identifier
     * @return true if exists
     */
    boolean has(String toolId);

    /**
     * List all registered tools.
     *
     * @return list of tools
     */
    List<Tool> all();

    /**
     * List tools by group.
     *
     * @param group the group name
     * @return list of tools in group
     */
    List<Tool> byGroup(String group);

    /**
     * Get all tool groups.
     *
     * @return list of group names
     */
    List<String> groups();

    /**
     * Enable a tool.
     *
     * @param toolId the tool identifier
     */
    void enable(String toolId);

    /**
     * Disable a tool.
     *
     * @param toolId the tool identifier
     */
    void disable(String toolId);

    /**
     * Check if a tool is enabled.
     *
     * @param toolId the tool identifier
     * @return true if enabled
     */
    boolean isEnabled(String toolId);

    /**
     * Get count of all tools.
     *
     * @return total count
     */
    int count();

    /**
     * Get count of enabled tools.
     *
     * @return enabled count
     */
    int activeCount();

    /**
     * Convert tools to a format suitable for LLM tool definitions.
     *
     * @return list of tool definitions
     */
    List<Map<String, Object>> toToolDefinitions();

    /**
     * Get a human-readable listing of all tools.
     *
     * @return formatted tool list
     */
    String listTools();

    /**
     * Execute a tool by ID with caching.
     *
     * @param toolId the tool identifier
     * @param params tool input parameters
     * @param context tool execution context
     * @return Uni containing tool execution result
     */
    Uni<Map<String, Object>> execute(String toolId,
                                      Map<String, Object> params,
                                      ToolContext context);
}
