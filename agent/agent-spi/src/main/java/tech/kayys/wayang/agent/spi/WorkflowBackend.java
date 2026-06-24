package tech.kayys.wayang.agent.spi;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Backend-agnostic workflow interface.
 */
public interface WorkflowBackend {

    String name();
    String version();

    Uni<WorkflowTypes.WorkflowRun> createRun(WorkflowTypes.CreateRunRequest request);
    Uni<WorkflowTypes.RunResponse> startRun(WorkflowTypes.WorkflowRunId runId, Map<String, Object> inputs);
    Uni<WorkflowTypes.RunResponse> suspendRun(WorkflowTypes.WorkflowRunId runId);
    Uni<WorkflowTypes.RunResponse> resumeRun(WorkflowTypes.WorkflowRunId runId, Map<String, Object> inputs);
    Uni<WorkflowTypes.RunResponse> cancelRun(WorkflowTypes.WorkflowRunId runId, String reason);
    Uni<WorkflowTypes.RunResponse> signalRun(WorkflowTypes.WorkflowRunId runId, WorkflowTypes.Signal signal);
    Uni<WorkflowTypes.RunHistory> getRunHistory(WorkflowTypes.WorkflowRunId runId);
    Uni<WorkflowTypes.RunStatus> getRunStatus(WorkflowTypes.WorkflowRunId runId);
    Uni<java.util.List<WorkflowTypes.WorkflowRun>> listRuns(String tenantId, int limit);

    boolean isHealthy();
    WorkflowTypes.WorkflowCapabilities capabilities();

    default void initialize(Map<String, Object> config) {}
    default void shutdown() {}
}
