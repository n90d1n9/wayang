package tech.kayys.wayang.tool.executor;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.plugin.PluginContext;
import tech.kayys.gamelan.engine.plugin.PluginException;
import tech.kayys.gamelan.plugin.executor.ExecutorPlugin;
import tech.kayys.gamelan.engine.plugin.PluginMetadata;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all tool executors.
 * Bridges Gamelan's ExecutorPlugin with Wayang's tool execution logic.
 */
public abstract class AbstractToolExecutor implements ExecutorPlugin {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(PluginContext context) throws PluginException {
        // Default no-op initialization
    }

    @Override
    public void start() throws PluginException {
        // Default no-op start
    }

    @Override
    public void stop() throws PluginException {
        // Default no-op stop
    }

    @Override
    public boolean canHandle(NodeExecutionTask task) {
        return true;
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata(
                getExecutorType(),
                "Tool Executor for " + getExecutorType(),
                "1.0.0",
                "Wayang",
                "Executor for " + getExecutorType() + " tool operations",
                List.of(),
                Map.of()
        );
    }

    protected NodeExecutionResult success(NodeExecutionTask task, Map<String, Object> output, Instant startedAt) {
        return SimpleNodeExecutionResult.success(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                output,
                task.token(),
                Duration.between(startedAt, Instant.now())
        );
    }

    protected NodeExecutionResult failure(NodeExecutionTask task, String error, Instant startedAt) {
        return SimpleNodeExecutionResult.success(
                task.runId(),
                task.nodeId(),
                task.attempt(),
                Map.of("error", error, "success", false),
                task.token(),
                Duration.between(startedAt, Instant.now())
        );
    }
}
