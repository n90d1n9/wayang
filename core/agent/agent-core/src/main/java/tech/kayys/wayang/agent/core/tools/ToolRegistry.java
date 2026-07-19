package tech.kayys.wayang.agent.core.tools;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Registry for agent tools.
 *
 * <p>Manages the lifecycle of {@link Tool} instances that are available
 * to agents at runtime. Tools can be registered programmatically or
 * discovered via CDI injection.
 */
@ApplicationScoped
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final Set<String> disabledTools = ConcurrentHashMap.newKeySet();

    @Inject
    Instance<Tool> discoveredTools;

    /**
     * Register a tool by its ID.
     *
     * @param tool the tool to register
     */
    public void register(Tool tool) {
        tools.put(tool.id(), tool);
    }

    /**
     * Get a tool by its ID.
     *
     * @param id the tool identifier
     * @return Optional containing the tool, or empty if not found
     */
    public Optional<Tool> getTool(String id) {
        if (disabledTools.contains(id)) {
            return Optional.empty();
        }
        Tool tool = tools.get(id);
        if (tool != null) {
            return Optional.of(tool);
        }
        // Fallback: search CDI-discovered tools
        for (Tool discovered : discoveredTools) {
            if (id.equals(discovered.id())) {
                return Optional.of(discovered);
            }
        }
        return Optional.empty();
    }

    /**
     * List all available tool definitions (metadata only, for prompt injection).
     *
     * @return list of tool definition maps (id, name, description, inputSchema)
     */
    public List<Map<String, Object>> getToolDefinitions() {
        return getAllTools().stream()
                .filter(t -> !disabledTools.contains(t.id()))
                .map(t -> Map.<String, Object>of(
                        "id", t.id(),
                        "name", t.name(),
                        "description", t.description(),
                        "inputSchema", t.inputSchema()
                ))
                .collect(Collectors.toList());
    }

    /**
     * List all available tools.
     *
     * @return Uni of all registered tools
     */
    public Uni<List<Tool>> listTools() {
        return Uni.createFrom().item(getAllTools());
    }

    /**
     * Execute a tool by ID with given arguments.
     *
     * @param toolId    the tool identifier
     * @param arguments input arguments
     * @param context   execution context map
     * @return Uni with result map
     */
    public Uni<Map<String, Object>> executeTool(String toolId, Map<String, Object> arguments, Map<Object, Object> context) {
        return Uni.createFrom().item(() -> {
            Optional<Tool> toolOpt = getTool(toolId);
            if (toolOpt.isEmpty()) {
                throw new IllegalArgumentException("Tool not found or disabled: " + toolId);
            }
            Tool tool = toolOpt.get();

            ToolResult result = tool.execute(arguments, ToolContext.defaults());
            if (result.success()) {
                Object data = result.data();
                if (data instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedMap = (Map<String, Object>) map;
                    return typedMap;
                } else {
                    return Map.of("output", data != null ? data.toString() : "");
                }
            } else {
                throw new RuntimeException("Tool execution failed: " + result.error());
            }
        });
    }

    /**
     * Enable a previously disabled tool.
     *
     * @param toolId the tool to enable
     */
    public void enable(String toolId) {
        disabledTools.remove(toolId);
    }

    /**
     * Disable a tool so it is not used during inference.
     *
     * @param toolId the tool to disable
     */
    public void disable(String toolId) {
        disabledTools.add(toolId);
    }

    /**
     * Check if a tool with the given ID is registered.
     *
     * @param id the tool identifier
     * @return true if the tool is registered and enabled
     */
    public boolean hasTools(String id) {
        return getTool(id).isPresent();
    }

    /**
     * Get all tool IDs for prompt injection.
     *
     * @return list of tool IDs
     */
    public List<String> getToolIds() {
        return getAllTools().stream()
                .filter(t -> !disabledTools.contains(t.id()))
                .map(Tool::id)
                .collect(Collectors.toList());
    }

    private List<Tool> getAllTools() {
        List<Tool> all = new ArrayList<>(tools.values());
        for (Tool discovered : discoveredTools) {
            if (!tools.containsKey(discovered.id())) {
                all.add(discovered);
            }
        }
        return all;
    }
}
