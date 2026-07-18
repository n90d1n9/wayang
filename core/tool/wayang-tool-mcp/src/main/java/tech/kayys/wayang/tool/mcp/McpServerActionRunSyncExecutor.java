package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

@ApplicationScoped
public class McpServerActionRunSyncExecutor implements McpServerActionExecutor {

    @Inject
    McpToolDiscoverySyncService discoverySyncService;

    @Inject
    McpToolServerHealthService serverHealthService;

    @Override
    public String actionCode() {
        return McpServerActionCatalog.ACTION_RUN_SYNC;
    }

    @Override
    public Uni<McpServerActionExecutionResult> execute(
            String requestId,
            McpServerActionPreview preview) {
        Instant startedAt = Instant.now();
        return mcpDiscoverySyncService().syncRegisteredServer(requestId, preview.serverName())
                .flatMap(syncResult -> refreshedActionQueue(requestId, preview.serverName())
                        .map(actionQueueAfter -> McpServerActionExecutionResult.executed(
                                preview,
                                syncResult,
                                actionQueueAfter,
                                startedAt,
                                Instant.now())))
                .onFailure().recoverWithItem(error ->
                        McpServerActionExecutionResult.failed(preview, error, startedAt, Instant.now()));
    }

    private Uni<McpServerActionQueue> refreshedActionQueue(String requestId, String serverName) {
        return mcpServerHealthService().summarize(
                requestId,
                McpServerHealthFilters.byServerName(serverName))
                .map(McpServerActionQueue::from);
    }

    private McpToolDiscoverySyncService mcpDiscoverySyncService() {
        return McpToolDiscoveryFallbacks.discoverySyncService(discoverySyncService);
    }

    private McpToolServerHealthService mcpServerHealthService() {
        return McpServerHealthFallbacks.serverHealthService(serverHealthService);
    }
}
