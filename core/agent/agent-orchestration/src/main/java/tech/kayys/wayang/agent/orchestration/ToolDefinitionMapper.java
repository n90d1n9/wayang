package tech.kayys.wayang.agent.orchestration;

import tech.kayys.wayang.agent.spi.InferenceTypes.ToolDefinition;
import tech.kayys.wayang.tools.spi.Tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts canonical Wayang tools into inference-facing agent tool definitions.
 */
public final class ToolDefinitionMapper {

    private ToolDefinitionMapper() {
    }

    public static ToolDefinition fromTool(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        return new ToolDefinition(
                tool.id(),
                tool.description(),
                safeSchema(tool.inputSchema()));
    }

    public static List<ToolDefinition> fromTools(List<? extends Tool> tools) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }
        return tools.stream()
                .filter(Objects::nonNull)
                .map(ToolDefinitionMapper::fromTool)
                .toList();
    }

    private static Map<String, Object> safeSchema(Map<String, Object> schema) {
        return schema == null ? Map.of() : Map.copyOf(schema);
    }
}
