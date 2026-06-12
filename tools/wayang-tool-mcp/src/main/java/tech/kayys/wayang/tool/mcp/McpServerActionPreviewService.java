package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class McpServerActionPreviewService {

    @Inject
    McpToolServerHealthService serverHealthService;

    public Uni<McpServerActionPreview> preview(
            String requestId,
            McpServerActionIdentity identity) {
        return mcpServerHealthService().summarize(
                requestId,
                McpServerHealthFilters.byServerAction(identity.serverName(), identity.actionCode()))
                .map(health -> McpServerActionPreview.from(
                        identity,
                        findAction(health, identity),
                        health == null ? List.of() : health.warnings()));
    }

    static McpToolServerHealth.ActionQueueItem findAction(
            McpToolServerHealth health,
            McpServerActionIdentity identity) {
        if (health == null || identity == null) {
            return null;
        }
        return health.actionQueue().stream()
                .filter(action -> identity.serverName().equalsIgnoreCase(action.serverName()))
                .filter(action -> identity.actionCode().equals(McpServerActionIdentity.normalizeActionCode(
                        action.code())))
                .findFirst()
                .orElse(null);
    }

    private McpToolServerHealthService mcpServerHealthService() {
        return McpServerHealthFallbacks.serverHealthService(serverHealthService);
    }
}
