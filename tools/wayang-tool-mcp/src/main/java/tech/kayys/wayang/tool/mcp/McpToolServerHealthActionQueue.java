package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Locale;

final class McpToolServerHealthActionQueue {

    private McpToolServerHealthActionQueue() {
    }

    static McpToolServerHealth.ActionQueueItem item(
            McpToolServerHealth.ServerHealth server,
            McpToolServerHealth.RecommendedAction action) {
        return new McpToolServerHealth.ActionQueueItem(
                id(server.serverName(), action.code()),
                server.serverName(),
                server.healthStatus(),
                server.transport(),
                server.endpoint(),
                action.code(),
                action.severity(),
                priority(action),
                action.safeToAutomate(),
                action.message(),
                action.actionHint(),
                action.operation(),
                action.executionMode());
    }

    static List<McpToolServerHealth.ActionQueueItem> sorted(
            List<McpToolServerHealth.ActionQueueItem> actionQueue) {
        return actionQueue.stream()
                .sorted(McpToolServerHealthActionQueue::compare)
                .toList();
    }

    static Window window(
            List<McpToolServerHealth.ActionQueueItem> actionQueue,
            Integer offset,
            Integer limit) {
        int total = actionQueue.size();
        int effectiveOffset = offset == null ? 0 : Math.min(Math.max(0, offset), total);
        if (limit == null) {
            return new Window(actionQueue.subList(effectiveOffset, total), effectiveOffset, null, total);
        }
        int effectiveLimit = Math.max(0, limit);
        int endExclusive = Math.min(total, effectiveOffset + effectiveLimit);
        return new Window(
                actionQueue.subList(effectiveOffset, endExclusive),
                effectiveOffset,
                effectiveLimit,
                total);
    }

    static int priority(McpToolServerHealth.RecommendedAction action) {
        int base = McpIssueSeverity.rank(action.severity()) * 100;
        return action.safeToAutomate() ? base + 10 : base;
    }

    static String id(String serverName, String actionCode) {
        String serverPart = serverName == null ? "" : serverName.trim();
        String actionPart = actionCode == null ? "" : actionCode.trim();
        return serverPart + ":" + actionPart;
    }

    private static int compare(
            McpToolServerHealth.ActionQueueItem left,
            McpToolServerHealth.ActionQueueItem right) {
        int priority = Integer.compare(right.priority(), left.priority());
        if (priority != 0) {
            return priority;
        }
        return stableKey(left.serverName()).compareTo(stableKey(right.serverName()));
    }

    private static String stableKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    record Window(
            List<McpToolServerHealth.ActionQueueItem> items,
            int offset,
            Integer limit,
            int total) {

        Window {
            items = List.copyOf(items);
        }

        int returned() {
            return items.size();
        }

        boolean truncated() {
            return offset + returned() < total;
        }
    }
}
