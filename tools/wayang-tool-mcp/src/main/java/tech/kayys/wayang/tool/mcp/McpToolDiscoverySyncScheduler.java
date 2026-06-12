package tech.kayys.wayang.tool.mcp;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class McpToolDiscoverySyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(McpToolDiscoverySyncScheduler.class);

    @Inject
    McpToolDiscoverySyncService syncService;

    @Scheduled(every = "60s", delayed = "25s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runScheduledLiveMcpToolSync() {
        try {
            syncService.syncScheduled()
                    .subscribe()
                    .with(result -> LOG.debug(
                                    "Live MCP tool scheduled sync: scanned={}, imported={}, warnings={}",
                                    result.scanned(),
                                    result.imported(),
                                    result.warnings().size()),
                            error -> LOG.warn("Live MCP tool scheduled sync failed: {}", error.getMessage()));
        } catch (Exception error) {
            LOG.warn("Live MCP tool scheduled sync dispatch failed: {}", error.getMessage());
        }
    }
}
