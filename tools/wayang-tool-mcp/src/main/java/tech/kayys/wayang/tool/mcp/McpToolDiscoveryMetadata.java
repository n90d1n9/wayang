package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpToolDiscoveryMetadata {

    static final String PAGES = "pages";
    static final String TOOL_COUNT = "toolCount";

    private McpToolDiscoveryMetadata() {
    }

    static Map<String, Object> pages(int pages) {
        return Map.of(PAGES, pages);
    }

    static Map<String, Object> success(String endpoint, int pages, int toolCount) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(McpHttpMetadata.ENDPOINT, endpoint);
        metadata.put(PAGES, pages);
        metadata.put(TOOL_COUNT, toolCount);
        return McpMaps.copy(metadata);
    }
}
