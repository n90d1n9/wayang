package tech.kayys.wayang.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.execution.ExecutionToken;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionStatus;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.run.RetryPolicy;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;
import tech.kayys.wayang.hitl.service.HumanTaskExecutor;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HumanTaskWorkflowExecutorTest {

    @Test
    void executeReturnsPendingResultAndDelegatesToHumanTaskExecutor() {
        HumanTaskExecutor humanTaskExecutor = mock(HumanTaskExecutor.class);
        when(humanTaskExecutor.execute("run-123", "hitl-node-1", Map.of(
                "taskType", "approval",
                "assignTo", "ops-team",
                "title", "Approve deployment")))
                .thenReturn(Uni.createFrom().voidItem());

        HumanTaskWorkflowExecutor executor = new HumanTaskWorkflowExecutor();
        executor.humanTaskExecutor = humanTaskExecutor;
        executor.objectMapper = new ObjectMapper();

        NodeExecutionTask task = createTask(
                "run-123",
                "hitl-node-1",
                Map.of(
                        "taskType", "approval",
                        "assignTo", "ops-team",
                        "title", "Approve deployment"));

        NodeExecutionResult result = executor.execute(task).await().indefinitely();

        assertEquals(NodeExecutionStatus.PENDING, result.status());
        assertEquals("PENDING", result.output().get("status"));
        assertEquals("approval", result.output().get("taskType"));
        assertEquals("ops-team", result.output().get("assignTo"));
        assertTrue(String.valueOf(result.output().get("message")).contains("awaiting"));

        verify(humanTaskExecutor).execute("run-123", "hitl-node-1", task.context());
    }

    private NodeExecutionTask createTask(String runIdValue, String nodeIdValue, Map<String, Object> context) {
        WorkflowRunId runId = new WorkflowRunId(runIdValue);
        NodeId nodeId = new NodeId(nodeIdValue);
        int attempt = 1;
        return new NodeExecutionTask(
                runId,
                nodeId,
                attempt,
                ExecutionToken.create(runId, nodeId, attempt, Duration.ofMinutes(5)),
                context,
                RetryPolicy.none());
    }
}
