package tech.kayys.wayang.tool.mcp;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class McpServerActionExecutionHistoryPruneScheduler {

    private static final Logger LOG =
            LoggerFactory.getLogger(McpServerActionExecutionHistoryPruneScheduler.class);

    private final McpServerActionExecutionHistoryService fallbackService =
            new McpServerActionExecutionHistoryService();

    @Inject
    McpServerActionExecutionHistoryService actionExecutionHistoryService;

    @ConfigProperty(name = "wayang.mcp.action-history.scheduled-prune.enabled", defaultValue = "true")
    boolean enabled = true;

    @Scheduled(
            every = "{wayang.mcp.action-history.scheduled-prune.interval:5m}",
            delayed = "{wayang.mcp.action-history.scheduled-prune.delayed:50s}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runScheduledActionExecutionHistoryPrune() {
        McpHistoryPruneSchedulerSupport.dispatch(
                "MCP server-action execution history scheduled prune",
                this::pruneExpiredActionExecutionHistory,
                McpServerActionExecutionHistoryPruneResult::pruned,
                LOG);
    }

    Uni<McpServerActionExecutionHistoryPruneResult> pruneExpiredActionExecutionHistory() {
        return McpHistoryPruneSchedulerSupport.pruneIfEnabled(
                enabled,
                () -> actionExecutionHistoryService().pruneExpired(),
                McpServerActionExecutionHistoryPruneResult::new);
    }

    private McpServerActionExecutionHistoryService actionExecutionHistoryService() {
        return actionExecutionHistoryService == null ? fallbackService : actionExecutionHistoryService;
    }
}
