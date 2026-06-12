package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpTool;

import java.util.List;
import java.util.Map;

final class McpToolServerHealthInputs {

    private final Map<String, McpToolLifecycleCounts> toolCounts;
    private final Map<String, McpToolDiscoverySyncHistoryEntry> latestByServer;
    private final Map<String, McpToolDiscoverySyncHistoryEntry> successByServer;
    private final Map<String, McpToolDiscoverySyncHistoryEntry> errorByServer;
    private final Map<String, List<McpToolDiscoverySyncHistoryEntry>> recentByServer;

    private McpToolServerHealthInputs(
            Map<String, McpToolLifecycleCounts> toolCounts,
            Map<String, McpToolDiscoverySyncHistoryEntry> latestByServer,
            Map<String, McpToolDiscoverySyncHistoryEntry> successByServer,
            Map<String, McpToolDiscoverySyncHistoryEntry> errorByServer,
            Map<String, List<McpToolDiscoverySyncHistoryEntry>> recentByServer) {
        this.toolCounts = Map.copyOf(toolCounts);
        this.latestByServer = Map.copyOf(latestByServer);
        this.successByServer = Map.copyOf(successByServer);
        this.errorByServer = Map.copyOf(errorByServer);
        this.recentByServer = Map.copyOf(recentByServer);
    }

    static McpToolServerHealthInputs from(
            List<McpTool> tools,
            List<McpToolDiscoverySyncHistoryEntry> latest,
            List<McpToolDiscoverySyncHistoryEntry> latestSuccess,
            List<McpToolDiscoverySyncHistoryEntry> latestError,
            List<McpToolDiscoverySyncHistoryEntry> recentHistory) {
        return new McpToolServerHealthInputs(
                McpToolServerHealthToolCounts.byServer(safeList(tools)),
                McpToolServerHealthSyncHistory.byServer(safeList(latest)),
                McpToolServerHealthSyncHistory.byServer(safeList(latestSuccess)),
                McpToolServerHealthSyncHistory.byServer(safeList(latestError)),
                McpToolServerHealthSyncHistory.listByServer(safeList(recentHistory)));
    }

    ServerInput forServer(String serverName) {
        String key = McpToolServerHealthSyncHistory.serverKey(serverName);
        return new ServerInput(
                toolCounts.getOrDefault(key, new McpToolLifecycleCounts()),
                latestByServer.get(key),
                successByServer.get(key),
                errorByServer.get(key),
                recentByServer.getOrDefault(key, List.of()));
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    record ServerInput(
            McpToolLifecycleCounts toolCounts,
            McpToolDiscoverySyncHistoryEntry latest,
            McpToolDiscoverySyncHistoryEntry latestSuccess,
            McpToolDiscoverySyncHistoryEntry latestError,
            List<McpToolDiscoverySyncHistoryEntry> recentHistory) {

        ServerInput {
            toolCounts = toolCounts == null ? new McpToolLifecycleCounts() : toolCounts;
            recentHistory = recentHistory == null ? List.of() : List.copyOf(recentHistory);
        }
    }
}
