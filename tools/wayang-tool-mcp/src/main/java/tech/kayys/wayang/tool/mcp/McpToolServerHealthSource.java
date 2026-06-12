package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.util.List;

final class McpToolServerHealthSource {

    private static final int HISTORY_SCAN_LIMIT = 200;

    private McpToolServerHealthSource() {
    }

    static Uni<Snapshot> load(
            String requestId,
            McpServerRegistryRepository serverRegistryRepository,
            ToolRepository toolRepository,
            McpToolDiscoverySyncService syncService) {
        Uni<List<McpServerRegistry>> servers = serverRegistryRepository.listByRequestId(requestId);
        Uni<List<McpTool>> tools = toolRepository.findByRequestId(requestId);
        Uni<List<McpToolDiscoverySyncHistoryEntry>> latest = latestHistory(syncService, requestId, null);
        Uni<List<McpToolDiscoverySyncHistoryEntry>> latestSuccess =
                latestHistory(syncService, requestId, McpToolDiscoverySyncStatuses.SUCCESS);
        Uni<List<McpToolDiscoverySyncHistoryEntry>> latestError =
                latestHistory(syncService, requestId, McpToolDiscoverySyncStatuses.ERROR);
        Uni<List<McpToolDiscoverySyncHistoryEntry>> recentHistory = recentHistory(syncService, requestId);

        return Uni.combine().all().unis(servers, tools, latest, latestSuccess, latestError, recentHistory).asTuple()
                .map(tuple -> Snapshot.from(
                        tuple.getItem1(),
                        tuple.getItem2(),
                        tuple.getItem3(),
                        tuple.getItem4(),
                        tuple.getItem5(),
                        tuple.getItem6()));
    }

    private static Uni<List<McpToolDiscoverySyncHistoryEntry>> latestHistory(
            McpToolDiscoverySyncService syncService,
            String requestId,
            String status) {
        if (syncService == null) {
            return Uni.createFrom().item(List.of());
        }
        return syncService.listLatestHistory(requestId, null, status, HISTORY_SCAN_LIMIT);
    }

    private static Uni<List<McpToolDiscoverySyncHistoryEntry>> recentHistory(
            McpToolDiscoverySyncService syncService,
            String requestId) {
        if (syncService == null) {
            return Uni.createFrom().item(List.of());
        }
        return syncService.listHistory(requestId, null, null, HISTORY_SCAN_LIMIT);
    }

    record Snapshot(
            List<McpServerRegistry> servers,
            McpToolServerHealthInputs inputs) {

        Snapshot {
            servers = servers == null ? List.of() : List.copyOf(servers);
            inputs = inputs == null ? McpToolServerHealthInputs.from(null, null, null, null, null) : inputs;
        }

        private static Snapshot from(
                List<McpServerRegistry> servers,
                List<McpTool> tools,
                List<McpToolDiscoverySyncHistoryEntry> latest,
                List<McpToolDiscoverySyncHistoryEntry> latestSuccess,
                List<McpToolDiscoverySyncHistoryEntry> latestError,
                List<McpToolDiscoverySyncHistoryEntry> recentHistory) {
            return new Snapshot(
                    servers,
                    McpToolServerHealthInputs.from(
                            tools,
                            latest,
                            latestSuccess,
                            latestError,
                            recentHistory));
        }
    }
}
