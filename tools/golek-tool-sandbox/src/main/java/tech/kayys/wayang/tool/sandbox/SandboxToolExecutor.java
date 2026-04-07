package tech.kayys.wayang.tool.sandbox;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.wayang.tool.executor.AbstractToolExecutor;
import tech.kayys.wayang.tool.node.ToolNodeTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Executor for Sandbox tools.
 */
@ApplicationScoped
public class SandboxToolExecutor extends AbstractToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(SandboxToolExecutor.class);

    @Override
    public String getExecutorType() {
        return ToolNodeTypes.TOOL_SANDBOX;
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context();
        String code = (String) context.get("code");
        String language = (String) context.getOrDefault("language", "python");

        if (code == null || code.isBlank()) {
            return Uni.createFrom().item(failure(task, "Code is required for Sandbox execution", startedAt));
        }

        LOG.info("Executing {} code in sandbox", language);

        // TODO: Implement actual sandbox execution
        return Uni.createFrom().item(success(task, Map.of(
                "status", "success",
                "language", language,
                "output",
                "Sandbox execution result placeholder for: " + code.substring(0, Math.min(code.length(), 20)) + "..."),
                startedAt));
    }
}
