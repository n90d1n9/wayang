package tech.kayys.wayang.agent.backend.gamelan;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import tech.kayys.gamelan.engine.execution.ExecutionHistory;
import tech.kayys.gamelan.engine.run.RunResponse;
import tech.kayys.gamelan.sdk.client.GamelanClient;
import tech.kayys.gamelan.sdk.client.GamelanClientConfig;
import tech.kayys.wayang.agent.spi.WorkflowBackend;
import tech.kayys.wayang.agent.spi.WorkflowTypes.CreateRunRequest;
import tech.kayys.wayang.agent.spi.WorkflowTypes.ExecutionEvent;
import tech.kayys.wayang.agent.spi.WorkflowTypes.RunHistory;
import tech.kayys.wayang.agent.spi.WorkflowTypes.RunStatus;
import tech.kayys.wayang.agent.spi.WorkflowTypes.Signal;
import tech.kayys.wayang.agent.spi.WorkflowTypes.WorkflowCapabilities;
import tech.kayys.wayang.agent.spi.WorkflowTypes.WorkflowRun;
import tech.kayys.wayang.agent.spi.WorkflowTypes.WorkflowRunId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adapts the current Gamelan workflow SDK to the backend-agnostic Wayang
 * {@link WorkflowBackend} contract.
 */
public class GamelanBackendAdapter implements WorkflowBackend {

    private static final Logger LOG = Logger.getLogger(GamelanBackendAdapter.class);

    private final GamelanClient gamelanClient;
    private volatile WorkflowCapabilities capabilities = defaultCapabilities("UNKNOWN");
    private volatile boolean initialized;

    public GamelanBackendAdapter(GamelanClient gamelanClient) {
        this.gamelanClient = Objects.requireNonNull(gamelanClient, "gamelanClient");
    }

    @Override
    public String name() {
        return "gamelan";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public Uni<WorkflowRun> createRun(CreateRunRequest request) {
        ensureInitialized();

        return gamelanClient.runs()
                .create(request.workflowId())
                .inputs(request.metadata())
                .correlationId(request.runId())
                .label("tenantId", request.tenantId())
                .execute()
                .map(response -> toWorkflowRun(response, request))
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to create Gamelan workflow run for workflow %s", request.workflowId());
                    return new RuntimeException("Failed to create workflow run: " + error.getMessage(), error);
                });
    }

    @Override
    public Uni<tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse> startRun(
            WorkflowRunId runId,
            Map<String, Object> inputs) {
        ensureInitialized();

        return gamelanClient.runs()
                .start(runId.id())
                .map(this::toRunResponse)
                .onFailure().transform(error -> wrap("start", runId, error));
    }

    @Override
    public Uni<tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse> suspendRun(WorkflowRunId runId) {
        ensureInitialized();

        return gamelanClient.runs()
                .suspend(runId.id())
                .reason("Suspended by Wayang agent")
                .execute()
                .map(this::toRunResponse)
                .onFailure().transform(error -> wrap("suspend", runId, error));
    }

    @Override
    public Uni<tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse> resumeRun(
            WorkflowRunId runId,
            Map<String, Object> inputs) {
        ensureInitialized();

        return gamelanClient.runs()
                .resume(runId.id())
                .data(inputs == null ? Map.of() : inputs)
                .execute()
                .map(this::toRunResponse)
                .onFailure().transform(error -> wrap("resume", runId, error));
    }

    @Override
    public Uni<tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse> cancelRun(
            WorkflowRunId runId,
            String reason) {
        ensureInitialized();

        return gamelanClient.runs()
                .cancel(runId.id(), reason)
                .replaceWith(() -> new tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse(
                        runId,
                        null,
                        new RunStatus(runId, "cancelled", 0, 0, 0, Instant.now(), Instant.now()),
                        Map.of(),
                        reason,
                        0,
                        Instant.now()))
                .onFailure().transform(error -> wrap("cancel", runId, error));
    }

    @Override
    public Uni<tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse> signalRun(
            WorkflowRunId runId,
            Signal signal) {
        ensureInitialized();

        return gamelanClient.runs()
                .signal(runId.id())
                .name(signal.name())
                .payload(signal.payload() == null ? Map.of() : signal.payload())
                .execute()
                .replaceWith(() -> new tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse(
                        runId,
                        null,
                        null,
                        Map.of(),
                        null,
                        0,
                        Instant.now()))
                .onFailure().transform(error -> wrap("signal", runId, error));
    }

    @Override
    public Uni<RunHistory> getRunHistory(WorkflowRunId runId) {
        ensureInitialized();

        return gamelanClient.runs()
                .getHistory(runId.id())
                .map(this::toRunHistory)
                .onFailure().transform(error -> wrap("history", runId, error));
    }

    @Override
    public Uni<RunStatus> getRunStatus(WorkflowRunId runId) {
        ensureInitialized();

        return gamelanClient.runs()
                .get(runId.id())
                .map(this::toRunStatus)
                .onFailure().transform(error -> wrap("status", runId, error));
    }

    @Override
    public boolean isHealthy() {
        return initialized;
    }

    @Override
    public WorkflowCapabilities capabilities() {
        ensureInitialized();
        return capabilities;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            return;
        }
        GamelanClientConfig clientConfig = gamelanClient.config();
        String transport = clientConfig != null && clientConfig.transport() != null
                ? clientConfig.transport().name()
                : "UNKNOWN";
        capabilities = defaultCapabilities(transport);
        initialized = true;
    }

    @Override
    public void shutdown() {
        if (!initialized) {
            return;
        }
        try {
            gamelanClient.close();
        } finally {
            initialized = false;
        }
    }

    private WorkflowRun toWorkflowRun(RunResponse response, CreateRunRequest request) {
        WorkflowRunId runId = workflowRunId(response);
        return new WorkflowRun(
                runId,
                firstNonBlank(response.getWorkflowId(), request.workflowId()),
                request.tenantId(),
                instantOrNow(response.getCreatedAt()),
                toRunStatus(response),
                request.metadata());
    }

    private tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse toRunResponse(RunResponse response) {
        return new tech.kayys.wayang.agent.spi.WorkflowTypes.RunResponse(
                workflowRunId(response),
                response.getWorkflowId(),
                toRunStatus(response),
                response.getOutputs() == null ? Map.of() : response.getOutputs(),
                response.getErrorMessage(),
                response.getDurationMs() == null ? 0 : response.getDurationMs(),
                instantOrNow(response.getCompletedAt()));
    }

    private RunStatus toRunStatus(RunResponse response) {
        WorkflowRunId runId = workflowRunId(response);
        return new RunStatus(
                runId,
                firstNonBlank(response.getStatus(), response.getPhase(), "unknown"),
                response.getNodesExecuted() == null ? 0 : response.getNodesExecuted(),
                response.getNodesTotal() == null ? 0 : response.getNodesTotal(),
                response.getDurationMs() == null ? 0 : response.getDurationMs(),
                instantOrNow(response.getStartedAt()),
                response.getCompletedAt());
    }

    private RunHistory toRunHistory(ExecutionHistory history) {
        WorkflowRunId runId = new WorkflowRunId(history.getRunId() == null
                ? ""
                : history.getRunId().value());

        List<ExecutionEvent> events = history.getEvents() == null
                ? List.of()
                : history.getEvents().stream()
                        .map(event -> new ExecutionEvent(
                                instantOrNow(event.getTimestamp()),
                                event.getEventType() == null ? "unknown" : event.getEventType().name(),
                                event.getSource(),
                                event.getSource(),
                                event.getPayload() == null ? Map.of() : event.getPayload(),
                                event.getError() == null ? null : event.getError().toString()))
                        .toList();

        var currentState = history.getCurrentState();
        long durationMs = history.getTotalDuration() == null ? 0 : history.getTotalDuration().toMillis();

        RunStatus finalStatus = new RunStatus(
                runId,
                currentState == null ? "unknown" : currentState.map(Enum::name).orElse("unknown"),
                0,
                history.getStatistics() == null ? 0 : history.getStatistics().getTotalNodeExecutions(),
                durationMs,
                instantOrNow(history.getCreated()),
                currentState != null && currentState.filter(state -> state.isTerminal()).isPresent()
                        ? history.getLastUpdated()
                        : null);

        return new RunHistory(
                runId,
                history.getWorkflowId() == null ? null : history.getWorkflowId().toString(),
                events,
                latestSnapshot(history.getInputSnapshots()),
                latestSnapshot(history.getOutputSnapshots()),
                finalStatus);
    }

    private Map<String, Object> latestSnapshot(Map<Instant, Map<String, Object>> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Map.of();
        }
        return snapshots.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(Map.of());
    }

    private WorkflowRunId workflowRunId(RunResponse response) {
        return new WorkflowRunId(response.getRunId() == null ? "" : response.getRunId());
    }

    private WorkflowCapabilities defaultCapabilities(String transport) {
        return new WorkflowCapabilities(
                true,
                true,
                true,
                true,
                1000,
                List.of(transport));
    }

    private RuntimeException wrap(String operation, WorkflowRunId runId, Throwable error) {
        LOG.errorf(error, "Failed to %s Gamelan workflow run %s", operation, runId);
        return new RuntimeException("Failed to " + operation + " workflow run: " + error.getMessage(), error);
    }

    private Instant instantOrNow(Instant instant) {
        return instant == null ? Instant.now() : instant;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialize(Map.of());
        }
    }
}
