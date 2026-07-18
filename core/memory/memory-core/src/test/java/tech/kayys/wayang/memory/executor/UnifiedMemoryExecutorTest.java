package tech.kayys.wayang.memory.executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;

import java.util.Map;

public class UnifiedMemoryExecutorTest {

    @InjectMocks
    UnifiedMemoryExecutor executor = new UnifiedMemoryExecutor() {
        @Override
        protected Uni<NodeExecutionResult> handleStore(NodeExecutionTask task, Map<String, Object> context, String agentId) {
            return Uni.createFrom().item(createSuccessResult(task, Map.of("memoryId", "1234"), java.time.Instant.now()));
        }

        @Override
        protected Uni<NodeExecutionResult> handleRetrieve(NodeExecutionTask task, Map<String, Object> context, String agentId) {
            return Uni.createFrom().nullItem();
        }
    };

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default to all enabled like application.properties
        executor.semanticEnabled = true;
        executor.episodicEnabled = true;
        executor.workingEnabled = true;
        executor.longTermEnabled = true;
        executor.shortTermEnabled = true;
    }

    @Test
    void testExecuteSucceedsWhenTypeIsEnabled() {
        NodeExecutionTask task = Mockito.mock(NodeExecutionTask.class);
        Mockito.when(task.runId()).thenReturn(WorkflowRunId.of("runId"));
        Mockito.when(task.nodeId()).thenReturn(NodeId.of("nodeId"));

        // Episodic memory requested, and operation is store
        Map<String, Object> context = Map.of(
                "memoryType", "episodic",
                "operation", "store",
                "content", "Remember this"
        );
        Mockito.when(task.context()).thenReturn(context);

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        Assertions.assertEquals(tech.kayys.gamelan.engine.node.NodeExecutionStatus.COMPLETED, result.status());
        Assertions.assertEquals("1234", result.output().get("memoryId"));
    }

    @Test
    void testExecuteFailsWhenTypeIsDisabled() {
        NodeExecutionTask task = Mockito.mock(NodeExecutionTask.class);
        Mockito.when(task.runId()).thenReturn(WorkflowRunId.of("runId"));
        Mockito.when(task.nodeId()).thenReturn(NodeId.of("nodeId"));

        // Disable episodic memory for this test
        executor.episodicEnabled = false;

        Map<String, Object> context = Map.of(
                "memoryType", "episodic",
                "operation", "store",
                "content", "Remember this false"
        );
        Mockito.when(task.context()).thenReturn(context);

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Must fail because semantic is disabled
        Assertions.assertEquals(tech.kayys.gamelan.engine.node.NodeExecutionStatus.FAILED, result.status());
        Assertions.assertEquals("Memory type 'episodic' is disabled.", result.getError().getMessage());
    }

    @Test
    void testExecuteDefaultsToSemanticWhenNoTypeProvided() {
        NodeExecutionTask task = Mockito.mock(NodeExecutionTask.class);
        Mockito.when(task.runId()).thenReturn(WorkflowRunId.of("runId"));
        Mockito.when(task.nodeId()).thenReturn(NodeId.of("nodeId"));

        // No memoryType provided in context, operation is store
        Map<String, Object> context = Map.of(
                "operation", "store",
                "content", "Remember this"
        );
        Mockito.when(task.context()).thenReturn(context);

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Defaults to semantic which is TRUE in setup, so it completes
        Assertions.assertEquals(tech.kayys.gamelan.engine.node.NodeExecutionStatus.COMPLETED, result.status());

        // Now let's disable semantic to prove it defaults to semantic
        executor.semanticEnabled = false;

        NodeExecutionResult failResult = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        Assertions.assertEquals(tech.kayys.gamelan.engine.node.NodeExecutionStatus.FAILED, failResult.status());
        Assertions.assertEquals("Memory type 'semantic' is disabled.", failResult.getError().getMessage());
    }
}
