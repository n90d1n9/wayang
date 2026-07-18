package tech.kayys.wayang.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.execution.ExecutionContext;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionStatus;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.hitl.service.HumanTaskExecutor;
import tech.kayys.wayang.hitl.schema.HumanTaskConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * WorkflowExecutor for Human-In-The-Loop (HITL) tasks.
 *
 * Bridges the Gamelan execution framework with the domain-level
 * {@link HumanTaskExecutor}.
 * Uses {@link HumanTaskConfig} as the strongly-typed configuration DTO
 * ensuring:
 * 1. JSON Schema in the catalog stays in sync with runtime behavior (single
 * source of truth).
 * 2. Config parsing is type-safe — no raw Map access in business logic.
 *
 * When a workflow reaches an HITL node this executor:
 * - Maps the node context into a {@link HumanTaskConfig} via Jackson.
 * - Delegates to {@link HumanTaskExecutor#execute} to persist the task,
 * send notifications, and schedule escalation.
 * - Returns a PENDING result so the workflow waits until the human
 * completes the task via the REST API.
 */
@ApplicationScoped
@Executor(executorType = "hitl-human-task", description = "Human-In-The-Loop executor: pauses a workflow run and waits for a human to approve, reject, or complete a task via the REST API.", supportedNodeTypes = {
                "hitl-node", "human-task-node" }, maxConcurrentTasks = 50)
public class HumanTaskWorkflowExecutor extends AbstractWorkflowExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(HumanTaskWorkflowExecutor.class);

        @Inject
        HumanTaskExecutor humanTaskExecutor;

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
                LOG.info("HITL executor invoked: runId={}, nodeId={}", task.runId().value(), task.nodeId().value());

                // Parse config map into strongly-typed DTO — no more raw Map.get() in business
                // logic
                HumanTaskConfig config = objectMapper.convertValue(task.context(), HumanTaskConfig.class);

                Instant start = Instant.now();

                // Delegate to domain service: persists the task, sends notifications, schedules
                // escalation.
                // The workflow is now PENDING until the human completes it via REST API.
                return humanTaskExecutor
                                .execute(task.runId().value(), task.nodeId().value(), task.context())
                                .map(ignored -> {
                                        Duration duration = Duration.between(start, Instant.now());
                                        Map<String, Object> output = Map.of(
                                                        "status", "PENDING",
                                                        "taskType",
                                                        config.getTaskType() != null ? config.getTaskType()
                                                                        : "approval",
                                                        "assignTo",
                                                        config.getAssignTo() != null ? config.getAssignTo() : "",
                                                        "message", "Human task created and awaiting completion");

                                        // Return PENDING — workflow engine resumes on callback
                                        return (NodeExecutionResult) new SimpleNodeExecutionResult(
                                                        task.runId(),
                                                        task.nodeId(),
                                                        task.attempt(),
                                                        NodeExecutionStatus.PENDING,
                                                        output,
                                                        null,
                                                        task.token(),
                                                        Instant.now(),
                                                        duration,
                                                        ExecutionContext.builder().variables(output).build(),
                                                        null,
                                                        Map.of());
                                })
                                .onFailure().invoke(error -> LOG.error("HITL task creation failed: runId={}, nodeId={}",
                                                task.runId().value(), task.nodeId().value(), error));
        }
}
