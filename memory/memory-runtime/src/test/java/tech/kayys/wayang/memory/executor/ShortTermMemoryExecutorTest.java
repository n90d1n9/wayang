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
import tech.kayys.wayang.memory.ShortTermMemoryExecutor;
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShortTermMemoryExecutor
 */
@QuarkusTest
class ShortTermMemoryExecutorTest {

    @Inject
    ShortTermMemoryExecutor executor;

    @Inject
    AgentMemory agentMemory;

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
    @DisplayName("Should store memory entry in short-term buffer")
    void testStoreMemory() {
        // Given
        Map<String, Object> context = Map.of(
                "agentId", "test-agent",
                "operation", "store",
                "content", "Test memory content",
                "memoryType", "short"
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
                    assertThat(output.get("memoryType")).isEqualTo("short");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should retrieve context from short-term buffer")
    void testRetrieveContext() {
        // Given
        // First store some entries
        Map<String, Object> storeContext = Map.of(
                "agentId", "test-agent-2",
                "operation", "store",
                "content", "Test context content",
                "memoryType", "short"
        );
        NodeExecutionTask storeTask = new NodeExecutionTask(runId, nodeId, 0, token, storeContext, RetryPolicy.DEFAULT);
        executor.execute(storeTask).await().indefinitely();

        // Then retrieve
        Map<String, Object> retrieveContext = Map.of(
                "agentId", "test-agent-2",
                "operation", "context",
                "memoryType", "short"
        );
        NodeExecutionTask retrieveTask = new NodeExecutionTask(runId, nodeId, 0, token, retrieveContext, RetryPolicy.DEFAULT);

        // When
        Uni<NodeExecutionResult> resultUni = executor.execute(retrieveTask);

        // Then
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    assertThat(result.status()).isEqualTo(NodeExecutionStatus.COMPLETED);
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("context");
                    assertThat((Integer) output.get("count")).isGreaterThanOrEqualTo(1);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should clear short-term buffer")
    void testClearBuffer() {
        // Given
        String agentId = "test-agent-clear";
        Map<String, Object> storeContext = Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "To be cleared",
                "memoryType", "short"
        );
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, storeContext, RetryPolicy.DEFAULT)).await().indefinitely();

        Map<String, Object> clearContext = Map.of(
                "agentId", agentId,
                "operation", "clear",
                "memoryType", "short"
        );
        NodeExecutionTask clearTask = new NodeExecutionTask(runId, nodeId, 0, token, clearContext, RetryPolicy.DEFAULT);

        // When
        Uni<NodeExecutionResult> resultUni = executor.execute(clearTask);

        // Then
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    assertThat(result.status()).isEqualTo(NodeExecutionStatus.COMPLETED);
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat(output.get("operation")).isEqualTo("clear");
                    assertThat((Integer) output.get("clearedCount")).isGreaterThanOrEqualTo(1);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should enforce window size limit")
    void testWindowSizeEnforcement() {
        // Given
        String agentId = "test-agent-window";
        int windowSize = 3;
        
        // Store more entries than window size
        for (int i = 0; i < 5; i++) {
            Map<String, Object> context = Map.of(
                    "agentId", agentId,
                    "operation", "store",
                    "content", "Entry " + i,
                    "memoryType", "short",
                    "limit", windowSize
            );
            executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT)).await().indefinitely();
        }

        // When - retrieve context
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "context",
                "memoryType", "short",
                "limit", windowSize
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat((Integer) output.get("count")).isEqualTo(windowSize);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should search within short-term buffer")
    void testSearch() {
        // Given
        String agentId = "test-agent-search";
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Apple is a fruit",
                "memoryType", "short"
        ), RetryPolicy.DEFAULT)).await().indefinitely();
        
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Carrots are vegetables",
                "memoryType", "short"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "search",
                "query", "fruit",
                "memoryType", "short"
        );
        NodeExecutionTask task = new NodeExecutionTask(runId, nodeId, 0, token, context, RetryPolicy.DEFAULT);

        // Then
        executor.execute(task).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(match(result -> {
                    Map<String, Object> output = result.output();
                    assertThat(output.get("success")).isEqualTo(true);
                    assertThat((Integer) output.get("count")).isEqualTo(1);
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should return failure for missing content")
    void testStoreMissingContent() {
        // Given
        Map<String, Object> context = Map.of(
                "agentId", "test-agent",
                "operation", "store",
                "memoryType", "short"
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
                    assertThat((String) output.get("error")).contains("content");
                }))
                .assertCompleted();
    }

    @Test
    @DisplayName("Should get memory statistics")
    void testGetStats() {
        // Given
        String agentId = "test-agent-stats";
        executor.execute(new NodeExecutionTask(runId, nodeId, 0, token, Map.of(
                "agentId", agentId,
                "operation", "store",
                "content", "Stats test",
                "memoryType", "short"
        ), RetryPolicy.DEFAULT)).await().indefinitely();

        // When
        Map<String, Object> context = Map.of(
                "agentId", agentId,
                "operation", "stats",
                "memoryType", "short"
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
                    assertThat(stats.get("memoryType")).isEqualTo("short");
                    assertThat((Integer) stats.get("currentSize")).isGreaterThan(0);
                }))
                .assertCompleted();
    }

    @SuppressWarnings("unchecked")
    private <T> T match(java.util.function.Consumer<T> consumer) {
        return (T) consumer;
    }
}
