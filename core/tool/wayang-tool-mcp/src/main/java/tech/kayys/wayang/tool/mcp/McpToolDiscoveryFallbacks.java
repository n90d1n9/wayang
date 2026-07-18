package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

final class McpToolDiscoveryFallbacks {

    private static final String DISCOVERY_SYNC_NOT_CONFIGURED =
            "MCP discovery sync service is not configured";

    private McpToolDiscoveryFallbacks() {
    }

    static McpToolDiscoveryClient discoveryClient(McpToolDiscoveryClient configured) {
        return configured == null
                ? request -> Uni.createFrom().item(McpToolDiscoveryResult.failure(
                        request.serverName(),
                        "MCP discovery client is not configured",
                        0,
                        Map.of()))
                : configured;
    }

    static McpToolDiscoveryImportService discoveryImportService(
            McpToolDiscoveryImportService configured) {
        return configured == null ? new McpToolDiscoveryImportService() {
            @Override
            public Uni<McpToolDiscoveryImportResult> discoverAndImport(
                    String requestId,
                    McpToolDiscoveryImportRequest request) {
                return Uni.createFrom().item(McpToolDiscoveryImportResult.failure(
                        request.serverName(),
                        request.effectiveNamespace(),
                        "MCP discovery import service is not configured",
                        Map.of()));
            }
        } : configured;
    }

    static McpToolDiscoverySyncService discoverySyncService(
            McpToolDiscoverySyncService configured) {
        return configured == null ? new McpToolDiscoverySyncService() {
            @Override
            public Uni<McpToolDiscoverySyncResult> syncScheduled() {
                return Uni.createFrom().item(McpToolDiscoverySyncResults.warning(DISCOVERY_SYNC_NOT_CONFIGURED));
            }

            @Override
            public Uni<McpToolDiscoverySyncResult> syncRegisteredServer(
                    String requestId,
                    String serverName) {
                return Uni.createFrom().item(McpToolDiscoverySyncResults.warning(DISCOVERY_SYNC_NOT_CONFIGURED));
            }

            @Override
            public Uni<List<McpToolDiscoverySyncHistoryEntry>> listHistory(
                    String requestId,
                    String serverName,
                    String status,
                    int limit) {
                return Uni.createFrom().item(List.of());
            }

            @Override
            public Uni<List<McpToolDiscoverySyncHistoryEntry>> listLatestHistory(
                    String requestId,
                    String serverName,
                    String status,
                    int limit) {
                return Uni.createFrom().item(List.of());
            }

            @Override
            public Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
                    String requestId,
                    String serverName,
                    int limit) {
                return summarizeHistory(requestId, serverName, null, limit);
            }

            @Override
            public Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
                    String requestId,
                    String serverName,
                    String status,
                    int limit) {
                return Uni.createFrom().item(new McpToolDiscoverySyncHistorySummary(
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
            }
        } : configured;
    }
}
