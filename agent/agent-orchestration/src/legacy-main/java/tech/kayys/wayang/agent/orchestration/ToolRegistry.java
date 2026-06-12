package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.gollek.spi.tool.ToolDefinition;
import tech.kayys.golok.tools.ToolRegistry;
import tech.kayys.golok.tools.spi.Tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Tool registry for agent orchestration with Gollek SDK support.
 * 
 * This registry:
 * - Loads tools via ServiceLoader or direct instantiation
 * - Provides tool definitions in Gollek's ToolDefinition format
 * - Bridges between Golok tools and Gollek inference
 */
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    /**
     * Load tools via ServiceLoader mechanism.
     * Pass null to use default classpath scanning.
     */
    public void loadPlugins(ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        
        // Load AgentTool implementations
        ServiceLoader<AgentTool> serviceLoader = ServiceLoader.load(AgentTool.class, loader);
        for (AgentTool tool : serviceLoader) {
            registerAgentTool(tool);
        }
        
        log.info("Loaded {} tools via ServiceLoader", tools.size());
    }

    /**
     * Register an agent tool manually.
     */
    public void registerAgentTool(AgentTool tool) {
        tools.put(tool.name(), tool);
        log.debug("Registered agent tool: {}", tool.name());
    }

    /**
     * Get an agent tool by name.
     */
    public AgentTool getAgentTool(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name + ". Available tools: " + tools.keySet());
        }
        return tool;
    }

    /**
     * Convert tools to Gollek ToolDefinition format.
     */
    public List<ToolDefinition> toGollekToolDefinitions() {
        return tools.values().stream()
                .map(this::convertToToolDefinition)
                .collect(Collectors.toList());
    }

    /**
     * Convert an AgentTool to Gollek ToolDefinition.
     */
    private ToolDefinition convertToToolDefinition(AgentTool tool) {
        // Build input schema from tool's expected parameters
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        
        // If tool provides a schema, parse it
        if (tool.schema() != null && !tool.schema().isBlank()) {
            try {
                JsonNode schemaNode = MAPPER.readTree(tool.schema());
                if (schemaNode.has("properties")) {
                    schemaNode.get("properties").fields().forEachRemaining(entry -> {
                        properties.put(entry.getKey(), convertJsonNodeToMap(entry.getValue()));
                    });
                }
                if (schemaNode.has("required") && schemaNode.get("required").isArray()) {
                    schemaNode.get("required").forEach(node -> required.add(node.asText()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse schema for tool {}: {}", tool.name(), e.getMessage());
            }
        }

        return ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(buildInputSchema(properties, required))
                .build();
    }

    /**
     * Build input schema JSON for Gollek.
     */
    private Map<String, Object> buildInputSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /**
     * Convert JsonNode to a Map representation.
     */
    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        return MAPPER.convertValue(node, Map.class);
    }

    /**
     * Get all registered tool names.
     */
    public Set<String> toolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * Get count of registered tools.
     */
    public int size() {
        return tools.size();
    }

    /**
     * Agent Tool interface for the orchestration system.
     * Tools should implement this interface to be auto-discovered.
     */
    public interface AgentTool {
        /**
         * Tool name (must be unique).
         */
        String name();

        /**
         * Tool description for LLM context.
         */
        String description();

        /**
         * JSON schema for tool inputs.
         */
        String schema();

        /**
         * Execute the tool with given input.
         * 
         * @param input JSON node containing tool parameters
         * @return tool execution result as string
         */
        String execute(JsonNode input);
    }
}
