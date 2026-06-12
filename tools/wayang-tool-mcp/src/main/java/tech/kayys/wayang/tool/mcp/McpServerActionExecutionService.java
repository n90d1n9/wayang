package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class McpServerActionExecutionService {

    @Inject
    McpServerActionExecutorRegistry executorRegistry;

    public Uni<McpServerActionExecutionResult> execute(
            String requestId,
            McpServerActionPreview preview) {
        if (!preview.found()) {
            return Uni.createFrom().item(McpServerActionExecutionResult.notFound(preview));
        }
        McpServerActionExecutorRegistry registry = mcpServerActionExecutorRegistry();
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview, registry::supports);
        if (!decision.allowed()) {
            return Uni.createFrom().item(McpServerActionExecutionResult.rejected(
                    preview,
                    decision.rejectionReason()));
        }
        return registry.execute(requestId, preview);
    }

    private McpServerActionExecutorRegistry mcpServerActionExecutorRegistry() {
        return executorRegistry == null ? new McpServerActionExecutorRegistry() : executorRegistry;
    }
}
