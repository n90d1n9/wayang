package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.McpTool;

import java.time.Duration;
import java.util.List;

final class McpToolServerHealthServiceTestHarness {
    private static final String DEFAULT_REQUEST_ID = "tenant-1";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String requestId;
    private final McpToolServerHealthService service;

    private McpToolServerHealthServiceTestHarness(
            String requestId,
            McpServerRegistryRepositoryTestDouble serverRepository,
            McpToolRepositoryTestDouble toolRepository,
            McpToolDiscoverySyncService syncService,
            boolean registryEnabled) {
        this.requestId = requestId;
        service = new McpToolServerHealthService();
        service.serverRegistryRepository = serverRepository;
        service.toolRepository = toolRepository;
        service.syncService = syncService;
        service.editionModeService = EditionModeServiceTestDouble.mcpRegistryDatabaseSupported(registryEnabled);
    }

    static McpToolServerHealthServiceTestHarness healthWith(
            McpServerRegistryRepositoryTestDouble serverRepository,
            McpToolRepositoryTestDouble toolRepository,
            McpToolDiscoverySyncService syncService) {
        return new McpToolServerHealthServiceTestHarness(
                DEFAULT_REQUEST_ID,
                serverRepository,
                toolRepository,
                syncService,
                true);
    }

    static McpToolServerHealthServiceTestHarness healthWith(
            List<McpServerRegistry> servers,
            List<McpTool> tools,
            McpToolDiscoverySyncService syncService) {
        return healthWith(
                new McpServerRegistryRepositoryTestDouble(servers),
                new McpToolRepositoryTestDouble(tools),
                syncService);
    }

    static McpToolServerHealthServiceTestHarness healthWithServers(
            McpServerRegistryRepositoryTestDouble serverRepository,
            McpToolDiscoverySyncService syncService) {
        return healthWith(serverRepository, new McpToolRepositoryTestDouble(List.of()), syncService);
    }

    static McpToolServerHealthServiceTestHarness disabledRegistryHealth() {
        return new McpToolServerHealthServiceTestHarness(
                DEFAULT_REQUEST_ID,
                new McpServerRegistryRepositoryTestDouble(List.of()),
                new McpToolRepositoryTestDouble(List.of()),
                new McpToolDiscoverySyncServiceTestDouble(List.of(), List.of(), List.of()),
                false);
    }

    McpToolServerHealth summarize() {
        return service.summarize(requestId, (String) null)
                .await().atMost(TIMEOUT);
    }

    McpToolServerHealth summarize(McpServerHealthFilters filters) {
        return service.summarize(requestId, filters)
                .await().atMost(TIMEOUT);
    }

    McpToolServerHealth summarizeServer(String serverName) {
        return service.summarize(requestId, serverName)
                .await().atMost(TIMEOUT);
    }
}
