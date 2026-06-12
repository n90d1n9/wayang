package tech.kayys.wayang.memory;

import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.execution.ExecutionToken;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.run.RetryPolicy;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;
import tech.kayys.wayang.memory.node.MemoryNodeTypes;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShortTermMemoryExecutorTest {

    @Test
    void keepsRecentEntriesInsideConfiguredWindow() {
        ShortTermMemoryExecutor executor = new ShortTermMemoryExecutor();
        executor.defaultWindowSize = 2;

        executor.execute(task(Map.of("operation", "STORE", "agentId", "agent-a", "content", "first")))
                .await().indefinitely();
        executor.execute(task(Map.of("operation", "STORE", "agentId", "agent-a", "content", "second")))
                .await().indefinitely();
        executor.execute(task(Map.of("operation", "STORE", "agentId", "agent-a", "content", "third")))
                .await().indefinitely();

        var result = executor.execute(task(Map.of("operation", "CONTEXT", "agentId", "agent-a")))
                .await().indefinitely();

        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("memoryType", "short")
                .containsEntry("count", 2);
        assertThat(entries(result.output()))
                .extracting(entry -> entry.get("content"))
                .containsExactly("second", "third");
    }

    @Test
    void exposesShortTermNodeDefinition() {
        ShortTermNodeProvider provider = new ShortTermNodeProvider();

        assertThat(provider.nodes()).hasSize(1);
        assertThat(provider.nodes().getFirst().type()).isEqualTo(MemoryNodeTypes.SHORT_TERM);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entries(Map<String, Object> output) {
        return (List<Map<String, Object>>) output.get("entries");
    }

    private NodeExecutionTask task(Map<String, Object> context) {
        WorkflowRunId runId = WorkflowRunId.generate();
        NodeId nodeId = NodeId.of("short-memory-node");
        return new NodeExecutionTask(
                runId,
                nodeId,
                1,
                ExecutionToken.create(runId, nodeId, 1, Duration.ofMinutes(1)),
                context,
                RetryPolicy.none());
    }
}
