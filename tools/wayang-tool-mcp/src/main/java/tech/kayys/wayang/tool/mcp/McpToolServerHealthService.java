package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;
import tech.kayys.wayang.tool.service.EditionModeService;

@ApplicationScoped
public class McpToolServerHealthService {

    @Inject
    McpServerRegistryRepository serverRegistryRepository;

    @Inject
    ToolRepository toolRepository;

    @Inject
    McpToolDiscoverySyncService syncService;

    @Inject
    EditionModeService editionModeService;

    public Uni<McpToolServerHealth> summarize(String requestId, String serverName) {
        return summarize(requestId, McpServerHealthFilters.byServerName(serverName));
    }

    public Uni<McpToolServerHealth> summarize(String requestId, McpServerHealthFilters filters) {
        McpServerHealthFilters effectiveFilters = filters == null
                ? McpServerHealthFilters.byServerName(null)
                : filters;
        if (editionModeService != null && !editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().item(McpToolServerHealthResponses.registryDatabaseModeDisabled());
        }
        if (serverRegistryRepository == null || toolRepository == null) {
            return Uni.createFrom().item(McpToolServerHealthResponses.registryNotConfigured());
        }

        return McpToolServerHealthSource.load(
                        requestId,
                        serverRegistryRepository,
                        toolRepository,
                        syncService)
                .map(snapshot -> McpToolServerHealthSummary.from(snapshot, effectiveFilters));
    }

}
