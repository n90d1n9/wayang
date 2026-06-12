package tech.kayys.wayang.tool.mcp;

import java.util.Map;

enum McpFailureType {
    HTTP,
    JSON_RPC,
    TOOL,
    TRANSPORT,
    PARSE;

    static final String METADATA_KEY = "failureType";

    static Map<String, Object> metadata(McpFailureType type) {
        return type == null ? Map.of() : Map.of(METADATA_KEY, type.name());
    }

    static void putMetadataValue(Map<String, Object> target, McpFailureType type) {
        if (type != null) {
            target.put(METADATA_KEY, type.name());
        }
    }

    static void copyMetadataValue(Map<String, Object> target, Map<String, Object> source) {
        if (source != null && source.get(METADATA_KEY) instanceof String failureType && !failureType.isBlank()) {
            target.put(METADATA_KEY, failureType);
        }
    }

    static String value(Map<String, Object> source) {
        if (source != null && source.get(METADATA_KEY) instanceof String failureType && !failureType.isBlank()) {
            return failureType;
        }
        return null;
    }
}
