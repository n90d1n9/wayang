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
import tech.kayys.wayang.memory.LongTermMemoryExecutor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LongTermMemoryExecutor
 */
@QuarkusTest
class LongTermMemoryExecutorTest {

    @Inject
    LongTermMemoryExecutor executor;

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
    @DisplayName("Should store memory with vector embedding")
    void testStoreMemory() {
        // Given
        Map<String, Object> context = Map.of(
                "agentId", "test-agent-lt",
                "operation", "store",
                "content", "Important long-term knowledge to remember",
                "memoryType", "longterm",
                "importance", 0.8
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
                    assertThat(output.get("memoryType")).isEqualTo("longterm");
                    assertThat(output.get("memoryId")).isNotNull();
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should search memories with query")
    void testSearchMemories() {
        // Given
        String agentId = "test-agent-search-lt";
        
        // Store test memories
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Java is a programming language",
                "memoryType", "longterm"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Python is used for data science",
                "memoryType", "longterm"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When - search
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "search",
                "query", "programming language",
                "memoryType", "longterm",
                "limit", 5
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("search");
                    assertThat(output.get("query")).isEqualTo("programming language");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should clear memory namespace")
    void testClearNamespace() {
        // Given
        String agentId = "test-agent-clear-lt";
        
        // Store a memory first
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "To be cleared",
                "memoryType", "longterm"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When - clear
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "clear",
                "memoryType", "longterm"
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

    @Test
    @DisplayName("Should get memory statistics")
    void testGetStats() {
        // Given
        String agentId = "test-agent-stats-lt";
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Stats test entry",
                "memoryType", "longterm"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "stats",
                "memoryType", "longterm"
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
                    assertThat(stats.get("memoryType")).isEqualTo("longterm");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should handle missing query for search")
    void testSearchMissingQuery() {
        // Given
        Map<String, Object> context = Map.of(
                "agentId", "test-agent",
                "operation", "search",
                "memoryType", "longterm"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // When
        Uni<NodeExecutionResult> resultUni = executor.execute(task);

        // Then
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(false);
                    assertThat((String) output.get("error")).contains("query");
                }))
                .assertCompleted();
    }

    @SuppressWarnings("unchecked")
    private <T> T match(java.util.function.Consumer<T> consumer) {
        return (T) consumer;
    }
}
