package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class McpServerActionExecutorRegistry {

    @Inject
    Instance<McpServerActionExecutor> executorBeans;

    List<McpServerActionExecutor> executors;

    public boolean supports(String actionCode) {
        return findExecutor(actionCode) != null;
    }

    public Uni<McpServerActionExecutionResult> execute(
            String requestId,
            McpServerActionPreview preview) {
        McpServerActionExecutor executor = findExecutor(preview.actionCode());
        if (executor == null) {
            return Uni.createFrom().item(McpServerActionExecutionResult.rejected(
                    preview,
                    McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE));
        }
        return executor.execute(requestId, preview);
    }

    static McpServerActionExecutor find(
            Iterable<McpServerActionExecutor> executors,
            String actionCode) {
        if (executors == null) {
            return null;
        }
        for (McpServerActionExecutor executor : executors) {
            if (executor != null && executor.supports(actionCode)) {
                return executor;
            }
        }
        return null;
    }

    private McpServerActionExecutor findExecutor(String actionCode) {
        return find(mcpServerActionExecutors(), actionCode);
    }

    private Iterable<McpServerActionExecutor> mcpServerActionExecutors() {
        if (executors != null) {
            return executors;
        }
        if (executorBeans == null) {
            return List.of(new McpServerActionRunSyncExecutor());
        }
        List<McpServerActionExecutor> configuredExecutors = executorBeans.stream().toList();
        return configuredExecutors.isEmpty()
                ? List.of(new McpServerActionRunSyncExecutor())
                : configuredExecutors;
    }
}
