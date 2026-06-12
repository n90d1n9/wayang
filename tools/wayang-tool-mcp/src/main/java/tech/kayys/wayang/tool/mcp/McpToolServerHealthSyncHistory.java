package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class McpToolServerHealthSyncHistory {

    private McpToolServerHealthSyncHistory() {
    }

    static Map<String, McpToolDiscoverySyncHistoryEntry> byServer(
            List<McpToolDiscoverySyncHistoryEntry> entries) {
        Map<String, McpToolDiscoverySyncHistoryEntry> result = new HashMap<>();
        for (McpToolDiscoverySyncHistoryEntry entry : entries) {
            result.put(serverKey(entry.serverName()), entry);
        }
        return result;
    }

    static Map<String, List<McpToolDiscoverySyncHistoryEntry>> listByServer(
            List<McpToolDiscoverySyncHistoryEntry> entries) {
        Map<String, List<McpToolDiscoverySyncHistoryEntry>> result = new HashMap<>();
        for (McpToolDiscoverySyncHistoryEntry entry : entries) {
            result.computeIfAbsent(serverKey(entry.serverName()), ignored -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    static int consecutiveFailures(List<McpToolDiscoverySyncHistoryEntry> entries) {
        int failures = 0;
        for (McpToolDiscoverySyncHistoryEntry entry : entries.stream()
                .sorted(McpToolServerHealthSyncHistory::compareDescending)
                .toList()) {
            if (!McpToolDiscoverySyncStatuses.isError(entry.status())) {
                break;
            }
            failures++;
        }
        return failures;
    }

    static String serverKey(String serverName) {
        return McpToolDiscoverySyncHistoryEntries.serverKey(serverName);
    }

    static Instant historyTime(McpToolDiscoverySyncHistoryEntry entry) {
        return McpToolDiscoverySyncHistoryEntries.historyTime(entry);
    }

    private static int compareDescending(
            McpToolDiscoverySyncHistoryEntry left,
            McpToolDiscoverySyncHistoryEntry right) {
        Instant leftTime = historyTime(left);
        Instant rightTime = historyTime(right);
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return rightTime.compareTo(leftTime);
    }
}
