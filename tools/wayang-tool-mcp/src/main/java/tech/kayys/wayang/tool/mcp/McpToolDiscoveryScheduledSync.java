package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.util.List;

final class McpToolDiscoveryScheduledSync {

    private McpToolDiscoveryScheduledSync() {
    }

    static Uni<McpToolDiscoverySyncResult> sync(
            McpServerRegistryRepository serverRegistryRepository,
            McpToolDiscoveryImportService importService,
            RegistrySyncHistoryRepository historyRepository) {
        if (serverRegistryRepository == null) {
            return Uni.createFrom().item(McpToolDiscoverySyncResults.warning(
                    McpToolDiscoverySyncMessages.serverRegistryNotConfigured()));
        }
        return serverRegistryRepository.listScheduledCandidates()
                .flatMap(servers -> sync(importService, historyRepository, servers));
    }

    static Uni<McpToolDiscoverySyncResult> sync(
            McpToolDiscoveryImportService importService,
            RegistrySyncHistoryRepository historyRepository,
            List<McpServerRegistry> servers) {
        Uni<McpToolDiscoverySyncResults.Accumulator> chain =
                Uni.createFrom().item(McpToolDiscoverySyncResults.accumulator());
        for (McpServerRegistry server : safeServers(servers)) {
            McpToolDiscoverySyncDueDecision decision = McpToolDiscoverySyncSchedule.dueDecision(server);
            if (!decision.due()) {
                if (decision.warning() != null) {
                    chain = chain.map(acc -> {
                        acc.addWarning(decision.warning());
                        return acc;
                    });
                }
                continue;
            }
            chain = chain.flatMap(acc -> McpToolDiscoverySyncRunner.syncServer(importService, historyRepository, server)
                    .map(result -> {
                        acc.addServerResult(result);
                        return acc;
                    }));
        }
        return chain.map(McpToolDiscoverySyncResults.Accumulator::toResult);
    }

    private static List<McpServerRegistry> safeServers(List<McpServerRegistry> servers) {
        return servers == null ? List.of() : servers;
    }
}
