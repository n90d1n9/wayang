package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Container for workflow-related value types.
 */
public final class WorkflowTypes {

    private WorkflowTypes() {}

    public record WorkflowRunId(String id) {
        @Override public String toString() { return id; }
    }

    public record WorkflowRun(WorkflowRunId runId, String workflowId, String tenantId, Instant createdAt, RunStatus status, Map<String, Object> metadata) {}

    public record CreateRunRequest(String workflowId, String tenantId, String runId, Map<String, Object> metadata) {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private String workflowId; private String tenantId = "default"; private String runId; private Map<String, Object> metadata;
            private Builder() {}
            public Builder workflowId(String v) { this.workflowId = v; return this; }
            public Builder tenantId(String v) { this.tenantId = v; return this; }
            public Builder runId(String v) { this.runId = v; return this; }
            public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }
            public CreateRunRequest build() { return new CreateRunRequest(workflowId, tenantId, runId, metadata != null ? metadata : Map.of()); }
        }
    }

    public record RunResponse(WorkflowRunId runId, String workflowId, RunStatus status, Map<String, Object> outputs, String error, long durationMs, Instant updatedAt) {}

    public record RunStatus(WorkflowRunId runId, String state, int currentStep, int totalSteps, long durationMs, Instant startedAt, Instant completedAt) {}

    public record RunHistory(WorkflowRunId runId, String workflowId, List<ExecutionEvent> events, Map<String, Object> inputs, Map<String, Object> outputs, RunStatus finalStatus) {}

    public record ExecutionEvent(Instant timestamp, String eventType, String nodeId, String nodeName, Map<String, Object> data, String error) {}

    public record Signal(String name, Map<String, Object> payload) {}

    public record WorkflowCapabilities(boolean supportsSuspension, boolean supportsSignals, boolean supportsHistory, boolean supportsVersioning, int maxConcurrentRuns, List<String> supportedTransports) {}
}
