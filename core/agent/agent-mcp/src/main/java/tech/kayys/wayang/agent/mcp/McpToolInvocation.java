package tech.kayys.wayang.agent.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral MCP tool invocation request.
 */
public record McpToolInvocation(
        McpToolDescriptor tool,
        Map<String, Object> arguments,
        Map<String, Object> context) {

    public McpToolInvocation {
        tool = Objects.requireNonNull(tool, "tool");
        arguments = McpMaps.copy(arguments);

        Map<String, Object> mergedContext = new LinkedHashMap<>(tool.metadata());
        mergedContext.putAll(McpMaps.copy(context));
        context = McpMaps.copy(mergedContext);
    }

    public McpToolInvocation(McpToolDescriptor tool, Map<String, Object> arguments) {
        this(tool, arguments, Map.of());
    }

    public static McpToolInvocation of(McpToolDescriptor tool, Map<String, Object> arguments) {
        return new McpToolInvocation(tool, arguments);
    }

    public String toolId() {
        return tool.id();
    }

    public String toolName() {
        return tool.name();
    }

    public String serverId() {
        return tool.serverId();
    }
}
