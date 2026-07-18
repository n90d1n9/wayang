package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Instant;

final class McpToolDiscoverySyncHistoryFixtures {

    private static final String REQUEST_ID = "tenant-1";

    private McpToolDiscoverySyncHistoryFixtures() {
    }

    static RegistrySyncHistory mcpHistory(
            String sourceRef,
            String status,
            Instant startedAt,
            Instant finishedAt) {
        return history(
                REQUEST_ID,
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                sourceRef,
                status,
                sourceRef + " synced",
                1,
                startedAt,
                finishedAt);
    }

    static RegistrySyncHistory mcpHistory(
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt,
            Instant finishedAt) {
        return history(
                REQUEST_ID,
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                sourceRef,
                status,
                message,
                itemsAffected,
                startedAt,
                finishedAt);
    }

    static RegistrySyncHistory mcpHistory(
            String requestId,
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt) {
        return mcpHistory(
                requestId,
                sourceRef,
                status,
                message,
                itemsAffected,
                startedAt,
                startedAt.plusMillis(10));
    }

    static RegistrySyncHistory mcpHistory(
            String requestId,
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt,
            Instant finishedAt) {
        return history(
                requestId,
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                sourceRef,
                status,
                message,
                itemsAffected,
                startedAt,
                finishedAt);
    }

    static RegistrySyncHistory history(
            String sourceKind,
            String sourceRef,
            String status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return history(
                REQUEST_ID,
                sourceKind,
                sourceRef,
                status,
                sourceRef + " synced",
                1,
                now,
                now.plusMillis(10));
    }

    static RegistrySyncHistory history(
            String requestId,
            String sourceKind,
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt) {
        return history(
                requestId,
                sourceKind,
                sourceRef,
                status,
                message,
                itemsAffected,
                startedAt,
                startedAt.plusMillis(10));
    }

    static RegistrySyncHistory history(
            String requestId,
            String sourceKind,
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt,
            Instant finishedAt) {
        RegistrySyncHistory history = new RegistrySyncHistory();
        history.setRequestId(requestId);
        history.setSourceKind(sourceKind);
        history.setSourceRef(sourceRef);
        history.setStatus(status);
        history.setMessage(message);
        history.setItemsAffected(itemsAffected);
        history.setStartedAt(startedAt);
        history.setFinishedAt(finishedAt);
        return history;
    }

    static McpToolDiscoverySyncHistoryEntry entry(
            String serverName,
            String status,
            String message,
            int itemsAffected,
            long durationMs,
            Instant startedAt,
            Instant finishedAt) {
        return new McpToolDiscoverySyncHistoryEntry(
                serverName,
                status,
                message,
                itemsAffected,
                durationMs,
                startedAt,
                finishedAt);
    }

    static McpToolDiscoverySyncHistoryEntry successEntry(
            String serverName,
            Instant startedAt,
            Instant finishedAt) {
        return entry(
                serverName,
                McpToolDiscoverySyncStatuses.SUCCESS,
                serverName + " synced",
                1,
                10,
                startedAt,
                finishedAt);
    }
}
