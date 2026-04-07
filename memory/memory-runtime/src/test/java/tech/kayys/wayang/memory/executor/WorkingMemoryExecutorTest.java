package tech.kayys.wayang.memory.executor;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeExecutionStatus;
import tech.kayys.gamelan.engine.execution.ExecutionToken;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.run.RetryPolicy;
import java.time.Duration;
import tech.kayys.wayang.memory.WorkingMemoryExecutor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkingMemoryExecutor
 */
@QuarkusTest
class WorkingMemoryExecutorTest {

    @Inject
    WorkingMemoryExecutor executor;

    private WorkflowRunId runId;
    private NodeId nodeId;
    private ExecutionToken token;

    @BeforeEach
    void setUp() {
        runId = WorkflowRunId.of("test-run-" + UUID.randomUUID());
        nodeId = NodeId.of("test-node");
        token = ExecutionToken.create(runId, nodeId, 0, Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should store entry in working memory slot")
    void testStoreMemory() {
        // Given
        Map<String, Object> context = Map.of(
                "agentId", "test-agent-wm",
                "operation", "store",
                "content", "Active task context",
                "memoryType", "working",
                "slot", "task-1"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // When
        Uni<NodeExecutionResult> resultUni = executor.execute(task);

        // Then
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    assertThat(result.status()).isEqualTo(NodeExecutionStatus.COMPLETED);
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("store");
                    assertThat(output.get("memoryType")).isEqualTo("working");
                    assertThat(output.get("slot")).isEqualTo("task-1");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should enforce capacity limit per slot")
    void testCapacityEnforcement() {
        // Given
        String agentId = "test-agent-cap";
        int capacity = 3;
        
        // Store more entries than capacity
        for (int i = 0; i < 5; i++) {
            executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                    "agentId", agentId,
                    "operation", "store",
                    "content", "Entry " + i,
                    "memoryType", "working",
                    "slot", "default",
                    "limit", capacity
            ), RetryPolicy.DEFAULT)).await().indefinitely();
        }

        // When - retrieve context
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "context",
                "memoryType", "working",
                "slot", "default"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat((Integer) output.get("count")).isEqualTo(capacity);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should search within working memory")
    void testSearch() {
        // Given
        String agentId = "test-agent-search-wm";
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Current task: implement feature X",
                "memoryType", "working"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Note: review code quality",
                "memoryType", "working"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "search",
                "query", "task",
                "memoryType", "working"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat((Integer) output.get("count")).isGreaterThan(0);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should update existing entry")
    void testUpdateEntry() {
        // Given
        String agentId = "test-agent-update";
        String memoryId = "test-entry-id";
        
        // Store initial entry
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Initial content",
                "memoryType", "working",
                "memoryId", memoryId
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When - update
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "update",
                "memoryId", memoryId,
                "content", "Updated content",
                "memoryType", "working"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("update");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should delete entry from working memory")
    void testDeleteEntry() {
        // Given
        String agentId = "test-agent-delete";
        String memoryId = "test-delete-id";
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "To be deleted",
                "memoryType", "working",
                "memoryId", memoryId
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When - delete
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "delete",
                "memoryId", memoryId,
                "memoryType", "working"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("deleted")).isEqualTo(true);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should get working memory statistics")
    void testGetStats() {
        // Given
        String agentId = "test-agent-stats-wm";
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Stats entry",
                "memoryType", "working"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "stats",
                "memoryType", "working"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("stats")).isNotNull();
                    Map<String, Object> stats = (Map<String, Object>) output.get("stats");
                    assertThat(stats.get("memoryType")).isEqualTo("working");
                    assertThat((Integer) stats.get("totalEntries")).isGreaterThan(0);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should clear working memory slot")
    void testClearSlot() {
        // Given
        String agentId = "test-agent-clear-wm";
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "To be cleared",
                "memoryType", "working",
                "slot", "clear-test"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When - clear
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "clear",
                "memoryType", "working",
                "slot", "clear-test"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("clear");
                }))
                .assertCompleted();
    }

    @SuppressWarnings("unchecked")
    private <T> T match(java.util.function.Consumer<T> consumer) {
        return (T) consumer;
    }
}
