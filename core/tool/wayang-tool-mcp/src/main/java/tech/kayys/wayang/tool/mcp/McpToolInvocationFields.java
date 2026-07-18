package tech.kayys.wayang.tool.mcp;

import java.util.Map;

final class McpToolInvocationFields {

    static final String KEY_ARGUMENTS = "arguments";
    static final String KEY_CUSTOM_DATA = "customData";
    static final String KEY_TOOL_ID = "toolId";

    private McpToolInvocationFields() {
    }

    static Map<String, Object> arguments(Map<String, Object> context) {
        if (context == null) {
            return Map.of();
        }
        return McpMaps.fromObject(context.get(KEY_ARGUMENTS));
    }

    static Map<String, Object> customData(Map<String, Object> context) {
        if (context == null) {
            return Map.of();
        }
        return McpMaps.fromObject(context.get(KEY_CUSTOM_DATA));
    }

    static String toolId(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        Object value = context.get(KEY_TOOL_ID);
        return value instanceof String string ? string : null;
    }
}
