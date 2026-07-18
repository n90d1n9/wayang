package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class McpToolServerHealthToolCounts {

    private McpToolServerHealthToolCounts() {
    }

    static Map<String, McpToolLifecycleCounts> byServer(List<McpTool> tools) {
        Map<String, McpToolLifecycleCounts> counts = new HashMap<>();
        for (McpTool tool : tools) {
            String serverName = McpToolLifecycle.serverName(tool);
            if (serverName == null) {
                continue;
            }
            counts.computeIfAbsent(serverKey(serverName), ignored -> new McpToolLifecycleCounts()).add(tool);
        }
        return counts;
    }

    static String serverKey(String serverName) {
        return McpToolServerHealthSyncHistory.serverKey(serverName);
    }
}
