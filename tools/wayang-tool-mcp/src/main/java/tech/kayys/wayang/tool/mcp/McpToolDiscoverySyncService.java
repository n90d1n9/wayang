package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;
import tech.kayys.wayang.tool.service.EditionModeService;

import java.util.List;

@ApplicationScoped
public class McpToolDiscoverySyncService {

    @Inject
    McpServerRegistryRepository serverRegistryRepository;

    @Inject
    McpToolDiscoveryImportService importService;

    @Inject
    RegistrySyncHistoryRepository historyRepository;

    @Inject
    EditionModeService editionModeService;

    public Uni<McpToolDiscoverySyncResult> syncScheduled() {
        if (editionModeService != null && !editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().item(McpToolDiscoverySyncResults.registryModeDisabled());
        }
        return McpToolDiscoveryScheduledSync.sync(
                serverRegistryRepository,
                importService,
                historyRepository);
    }

    public Uni<McpToolDiscoverySyncResult> syncRegisteredServer(String requestId, String serverName) {
        if (editionModeService != null && !editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().item(McpToolDiscoverySyncResults.registryModeDisabled());
        }
        return McpToolDiscoveryRegisteredServerSync.sync(
                serverRegistryRepository,
                importService,
                historyRepository,
                requestId,
                serverName);
    }

    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        return McpToolDiscoverySyncHistoryQueries.listHistory(
                historyRepository,
                requestId,
                serverName,
                status,
                limit);
    }

    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listLatestHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        return McpToolDiscoverySyncHistoryQueries.listLatestHistory(
                historyRepository,
                requestId,
                serverName,
                status,
                limit);
    }

    public Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
            String requestId,
            String serverName,
            int limit) {
        return summarizeHistory(requestId, serverName, null, limit);
    }

    public Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        return McpToolDiscoverySyncHistoryQueries.summarizeHistory(
                historyRepository,
                requestId,
                serverName,
                status,
                limit);
    }

}
