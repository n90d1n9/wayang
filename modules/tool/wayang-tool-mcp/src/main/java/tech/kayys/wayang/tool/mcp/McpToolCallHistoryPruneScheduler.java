package tech.kayys.wayang.tool.mcp;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class McpToolCallHistoryPruneScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(McpToolCallHistoryPruneScheduler.class);

    private final McpToolCallHistoryService fallbackService = new McpToolCallHistoryService();

    @Inject
    McpToolCallHistoryService toolCallHistoryService;

    @ConfigProperty(name = "wayang.mcp.tool-call-history.scheduled-prune.enabled", defaultValue = "true")
    boolean enabled = true;

    @Scheduled(
            every = "{wayang.mcp.tool-call-history.scheduled-prune.interval:5m}",
            delayed = "{wayang.mcp.tool-call-history.scheduled-prune.delayed:45s}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runScheduledToolCallHistoryPrune() {
        McpHistoryPruneSchedulerSupport.dispatch(
                "MCP tool-call history scheduled prune",
                this::pruneExpiredToolCallHistory,
                McpToolCallHistoryPruneResult::pruned,
                LOG);
    }

    Uni<McpToolCallHistoryPruneResult> pruneExpiredToolCallHistory() {
        return McpHistoryPruneSchedulerSupport.pruneIfEnabled(
                enabled,
                () -> toolCallHistoryService().pruneExpired(),
                McpToolCallHistoryPruneResult::new);
    }

    private McpToolCallHistoryService toolCallHistoryService() {
        return toolCallHistoryService == null ? fallbackService : toolCallHistoryService;
    }
}
