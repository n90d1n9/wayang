package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

public record McpDiscoveredTool(
        String id,
        String name,
        String title,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> metadata) {

    public McpDiscoveredTool {
        inputSchema = McpMaps.copy(inputSchema);
        outputSchema = McpMaps.copy(outputSchema);
        metadata = McpMaps.copy(metadata);
    }

    static McpDiscoveredTool from(String serverName, Object value) {
        Map<String, Object> raw = McpMaps.fromObject(value);
        String name = stringValue(raw.get(McpToolDiscoveryProtocol.FIELD_NAME));
        String normalizedName = name == null || name.isBlank() ? "unknown" : name.trim();
        String id = serverName == null || serverName.isBlank()
                ? normalizedName
                : serverName.trim() + ":" + normalizedName;

        Map<String, Object> metadata = new LinkedHashMap<>(raw);
        metadata.remove(McpToolDiscoveryProtocol.FIELD_INPUT_SCHEMA);
        metadata.remove(McpToolDiscoveryProtocol.FIELD_OUTPUT_SCHEMA);

        return new McpDiscoveredTool(
                id,
                normalizedName,
                stringValue(raw.get(McpToolDiscoveryProtocol.FIELD_TITLE)),
                stringValue(raw.get(McpToolDiscoveryProtocol.FIELD_DESCRIPTION)),
                defaultInputSchema(McpMaps.fromObject(raw.get(McpToolDiscoveryProtocol.FIELD_INPUT_SCHEMA))),
                McpMaps.fromObject(raw.get(McpToolDiscoveryProtocol.FIELD_OUTPUT_SCHEMA)),
                metadata);
    }

    private static Map<String, Object> defaultInputSchema(Map<String, Object> inputSchema) {
        return inputSchema.isEmpty() ? McpToolDiscoveryProtocol.defaultInputSchema() : inputSchema;
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : null;
    }
}
