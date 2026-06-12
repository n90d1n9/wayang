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

class WorkingMemoryExecutorTest {

    @Test
    void storesAndSearchesWorkingMemoryByAttention() {
        WorkingMemoryExecutor executor = new WorkingMemoryExecutor();
        executor.defaultCapacity = 3;
        executor.defaultTtlMinutes = 30;
        executor.enableAttention = true;

        executor.execute(task(Map.of(
                "operation", "STORE",
                "agentId", "agent-a",
                "memoryId", "low",
                "content", "draft deployment note",
                "attention", 0.2)))
                .await().indefinitely();
        executor.execute(task(Map.of(
                "operation", "STORE",
                "agentId", "agent-a",
                "memoryId", "high",
                "content", "deployment blocker",
                "attention", 0.9)))
                .await().indefinitely();

        var result = executor.execute(task(Map.of(
                "operation", "SEARCH",
                "agentId", "agent-a",
                "query", "deployment")))
                .await().indefinitely();

        assertThat(result.output())
                .containsEntry("success", true)
                .containsEntry("memoryType", "working")
                .containsEntry("count", 2);
        assertThat(entries(result.output()))
                .extracting(entry -> entry.get("id"))
                .containsExactly("high", "low");
    }

    @Test
    void exposesWorkingMemoryNodeDefinition() {
        WorkingNodeProvider provider = new WorkingNodeProvider();

        assertThat(provider.nodes()).hasSize(1);
        assertThat(provider.nodes().getFirst().type()).isEqualTo(MemoryNodeTypes.WORKING);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entries(Map<String, Object> output) {
        return (List<Map<String, Object>>) output.get("entries");
    }

    private NodeExecutionTask task(Map<String, Object> context) {
        WorkflowRunId runId = WorkflowRunId.generate();
        NodeId nodeId = NodeId.of("working-memory-node");
        return new NodeExecutionTask(
                runId,
                nodeId,
                1,
                ExecutionToken.create(runId, nodeId, 1, Duration.ofMinutes(1)),
                context,
                RetryPolicy.none());
    }
}
