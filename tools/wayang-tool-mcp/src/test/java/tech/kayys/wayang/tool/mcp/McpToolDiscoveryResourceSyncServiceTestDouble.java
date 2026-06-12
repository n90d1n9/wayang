package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;

final class McpToolDiscoveryResourceSyncServiceTestDouble extends McpToolDiscoverySyncService {
    private final McpToolDiscoverySyncResult scheduledResult;
    private final McpToolDiscoverySyncResult registeredResult;
    private final List<McpToolDiscoverySyncHistoryEntry> history;
    private final List<McpToolDiscoverySyncHistoryEntry> latestHistory;
    private final McpToolDiscoverySyncHistorySummary summary;
    private String lastRequestId;
    private String lastServerName;
    private String lastStatus;
    private int lastLimit;

    private McpToolDiscoveryResourceSyncServiceTestDouble(
            McpToolDiscoverySyncResult scheduledResult,
            McpToolDiscoverySyncResult registeredResult,
            List<McpToolDiscoverySyncHistoryEntry> history,
            List<McpToolDiscoverySyncHistoryEntry> latestHistory,
            McpToolDiscoverySyncHistorySummary summary) {
        this.scheduledResult = scheduledResult;
        this.registeredResult = registeredResult;
        this.history = history;
        this.latestHistory = latestHistory;
        this.summary = summary;
    }

    static McpToolDiscoveryResourceSyncServiceTestDouble scheduled(McpToolDiscoverySyncResult result) {
        return new McpToolDiscoveryResourceSyncServiceTestDouble(result, null, List.of(), List.of(), null);
    }

    static McpToolDiscoveryResourceSyncServiceTestDouble registered(McpToolDiscoverySyncResult result) {
        return new McpToolDiscoveryResourceSyncServiceTestDouble(null, result, List.of(), List.of(), null);
    }

    static McpToolDiscoveryResourceSyncServiceTestDouble history(
            List<McpToolDiscoverySyncHistoryEntry> history) {
        return new McpToolDiscoveryResourceSyncServiceTestDouble(null, null, history, List.of(), null);
    }

    static McpToolDiscoveryResourceSyncServiceTestDouble latestHistory(
            List<McpToolDiscoverySyncHistoryEntry> latestHistory) {
        return new McpToolDiscoveryResourceSyncServiceTestDouble(null, null, List.of(), latestHistory, null);
    }

    static McpToolDiscoveryResourceSyncServiceTestDouble summary(
            McpToolDiscoverySyncHistorySummary summary) {
        return new McpToolDiscoveryResourceSyncServiceTestDouble(null, null, List.of(), List.of(), summary);
    }

    String lastRequestId() {
        return lastRequestId;
    }

    String lastServerName() {
        return lastServerName;
    }

    String lastStatus() {
        return lastStatus;
    }

    int lastLimit() {
        return lastLimit;
    }

    @Override
    public Uni<McpToolDiscoverySyncResult> syncScheduled() {
        return Uni.createFrom().item(scheduledResult);
    }

    @Override
    public Uni<McpToolDiscoverySyncResult> syncRegisteredServer(
            String requestId,
            String serverName) {
        lastRequestId = requestId;
        lastServerName = serverName;
        return Uni.createFrom().item(registeredResult);
    }

    @Override
    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        captureHistoryRequest(requestId, serverName, status, limit);
        return Uni.createFrom().item(history);
    }

    @Override
    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listLatestHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        captureHistoryRequest(requestId, serverName, status, limit);
        return Uni.createFrom().item(latestHistory);
    }

    @Override
    public Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        captureHistoryRequest(requestId, serverName, status, limit);
        return Uni.createFrom().item(summary);
    }

    private void captureHistoryRequest(
            String requestId,
            String serverName,
            String status,
            int limit) {
        lastRequestId = requestId;
        lastServerName = serverName;
        lastStatus = status;
        lastLimit = limit;
    }
}
