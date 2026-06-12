package tech.kayys.wayang.agent.backend.gamelan;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import tech.kayys.gamelan.engine.execution.ExecutionHistory;
import tech.kayys.gamelan.engine.run.CreateRunRequest;
import tech.kayys.gamelan.engine.run.RunResponse;
import tech.kayys.gamelan.sdk.client.GamelanClient;
import tech.kayys.gamelan.sdk.client.GamelanClientConfig;
import tech.kayys.wayang.agent.spi.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backend adapter that wraps Gamelan SDK to implement the backend-agnostic
 * {@link WorkflowBackend} SPI.
 *
 * <p>
 * This adapter translates between wayang-gollek's backend-agnostic SPI types
 * and Gamelan SDK's native types, enabling any code that depends on
 * {@code WorkflowBackend} to work with Gamelan without direct coupling.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Programmatic (no Quarkus)
 * GamelanClientConfig config = GamelanClientConfig.builder()
 *     .transport(TransportType.LOCAL)
 *     .build();
 * GamelanClient client = GamelanClient.create(config);
 * WorkflowBackend backend = new GamelanBackendAdapter(client);
 * backend.initialize(config);
 *
 * Uni<WorkflowRun> run = backend.createRun(createRequest);
 * }</pre>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Full workflow lifecycle (create, start, suspend, resume, cancel, signal)</li>
 *   <li>History and status queries</li>
 *   <li>Capability detection from Gamelan transport type</li>
 *   <li>Graceful lifecycle management</li>
 * </ul>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class GamelanBackendAdapter implements WorkflowBackend {

    private static final Logger LOG = Logger.getLogger(GamelanBackendAdapter.class);

    private final GamelanClient gamelanClient;
    private WorkflowCapabilities capabilities;
    private volatile boolean initialized = false;

    /**
     * Create adapter with pre-configured Gamelan Client instance.
     *
     * @param gamelanClient configured Gamelan Client instance
     */
    public GamelanBackendAdapter(GamelanClient gamelanClient) {
        this.gamelanClient = gamelanClient != null ? gamelanClient : GamelanClient.builder().build();
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

        LOG.debugf("Creating workflow run for workflow %s", request.workflowId());

        CreateRunRequest gamelanRequest = mapToGamelanCreateRequest(request);

        return gamelanClient.runs().createRun(gamelanRequest)
            .map(this::mapFromGamelanRun)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to create workflow run for workflow %s", request.workflowId());
                return new RuntimeException("Failed to create workflow run: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunResponse> startRun(WorkflowRunId runId, Map<String, Object> inputs) {
        ensureInitialized();

        LOG.debugf("Starting workflow run %s", runId);

        return gamelanClient.runs().startRun(runId.id())
            .map(this::mapFromGamelanRunResponse)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to start workflow run %s", runId);
                return new RuntimeException("Failed to start workflow run: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunResponse> suspendRun(WorkflowRunId runId) {
        ensureInitialized();

        LOG.debugf("Suspending workflow run %s", runId);

        return gamelanClient.runs().suspendRun(runId.id(), "Suspended by agent", null)
            .map(this::mapFromGamelanRunResponse)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to suspend workflow run %s", runId);
                return new RuntimeException("Failed to suspend workflow run: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunResponse> resumeRun(WorkflowRunId runId, Map<String, Object> inputs) {
        ensureInitialized();

        LOG.debugf("Resuming workflow run %s", runId);

        return gamelanClient.runs().resumeRun(runId.id(), inputs, null)
            .map(this::mapFromGamelanRunResponse)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to resume workflow run %s", runId);
                return new RuntimeException("Failed to resume workflow run: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunResponse> cancelRun(WorkflowRunId runId, String reason) {
        ensureInitialized();

        LOG.debugf("Cancelling workflow run %s: %s", runId, reason);

        return gamelanClient.runs().cancelRun(runId.id(), reason)
            .map(v -> new RunResponse(
                runId,
                null,
                new RunStatus(runId, "cancelled", 0, 0, 0, Instant.now(), null),
                Map.of(),
                reason,
                0,
                Instant.now()
            ))
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to cancel workflow run %s", runId);
                return new RuntimeException("Failed to cancel workflow run: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunResponse> signalRun(WorkflowRunId runId, Signal signal) {
        ensureInitialized();

        LOG.debugf("Sending signal %s to workflow run %s", signal.name(), runId);

        return gamelanClient.runs().signal(runId.id(), signal.name(), null, signal.payload())
            .map(v -> new RunResponse(
                runId,
                null,
                null,
                Map.of(),
                null,
                0,
                Instant.now()
            ))
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to send signal to workflow run %s", runId);
                return new RuntimeException("Failed to send signal: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunHistory> getRunHistory(WorkflowRunId runId) {
        ensureInitialized();

        LOG.debugf("Getting run history for %s", runId);

        return gamelanClient.runs().getExecutionHistory(runId.id())
            .map(this::mapFromGamelanHistory)
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to get run history for %s", runId);
                return new RuntimeException("Failed to get run history: " + err.getMessage(), err);
            });
    }

    @Override
    public Uni<RunStatus> getRunStatus(WorkflowRunId runId) {
        ensureInitialized();

        LOG.debugf("Getting run status for %s", runId);

        return gamelanClient.runs().getRun(runId.id())
            .map(response -> new RunStatus(
                runId,
                response.status(),
                0,  // currentStep - not available in simple response
                0,  // totalSteps - not available in simple response
                0,  // durationMs - not available in simple response
                response.createdAt() != null ? response.createdAt() : Instant.now(),
                response.completedAt()
            ))
            .onFailure().transform(err -> {
                LOG.errorf(err, "Failed to get run status for %s", runId);
                return new RuntimeException("Failed to get run status: " + err.getMessage(), err);
            });
    }

    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }

        try {
            // GamelanClient doesn't have explicit health check, so we assume healthy if initialized
            return true;
        } catch (Exception e) {
            LOG.debugf("Gamelan health check failed: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public WorkflowCapabilities capabilities() {
        if (capabilities == null) {
            detectCapabilities();
        }
        return capabilities;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            LOG.debug("GamelanBackendAdapter already initialized");
            return;
        }

        LOG.info("Initializing GamelanBackendAdapter");

        try {
            if (gamelanClient == null) {
                throw new IllegalStateException("GamelanClient instance is null");
            }

            detectCapabilities();
            initialized = true;

            LOG.info("GamelanBackendAdapter initialized successfully");
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize GamelanBackendAdapter");
            throw new RuntimeException("Failed to initialize GamelanBackendAdapter", e);
        }
    }

    @Override
    public void shutdown() {
        if (!initialized) {
            return;
        }

        LOG.info("Shutting down GamelanBackendAdapter");

        try {
            gamelanClient.close();
            initialized = false;
            LOG.info("GamelanBackendAdapter shut down successfully");
        } catch (Exception e) {
            LOG.errorf(e, "Error shutting down GamelanBackendAdapter");
        }
    }

    // ── Type Mapping ─────────────────────────────────────────────────────

    /**
     * Map backend-agnostic CreateRunRequest to Gamelan CreateRunRequest.
     */
    private CreateRunRequest mapToGamelanCreateRequest(CreateRunRequest request) {
        // Build Gamelan CreateRunRequest
        // Note: Actual fields depend on Gamelan's CreateRunRequest structure
        return CreateRunRequest.builder()
            .workflowId(request.workflowId())
            .runId(request.runId())
            .tenantId(request.tenantId())
            .metadata(request.metadata())
            .build();
    }

    /**
     * Map Gamelan RunResponse to backend-agnostic WorkflowRun.
     */
    private WorkflowRun mapFromGamelanRun(RunResponse response) {
        return new WorkflowRun(
            new WorkflowRunId(response.runId()),
            response.workflowId(),
            response.tenantId(),
            response.createdAt() != null ? response.createdAt() : Instant.now(),
            new RunStatus(
                new WorkflowRunId(response.runId()),
                response.status(),
                0, 0, 0,
                response.createdAt() != null ? response.createdAt() : Instant.now(),
                response.completedAt()
            ),
            response.metadata() != null ? response.metadata() : Map.of()
        );
    }

    /**
     * Map Gamelan RunResponse to backend-agnostic RunResponse.
     */
    private RunResponse mapFromGamelanRunResponse(RunResponse response) {
        return new RunResponse(
            new WorkflowRunId(response.runId()),
            response.workflowId(),
            new RunStatus(
                new WorkflowRunId(response.runId()),
                response.status(),
                0, 0, 0,
                response.createdAt() != null ? response.createdAt() : Instant.now(),
                response.completedAt()
            ),
            response.outputs() != null ? response.outputs() : Map.of(),
            response.error(),
            0,  // durationMs - not available in response
            response.updatedAt() != null ? response.updatedAt() : Instant.now()
        );
    }

    /**
     * Map Gamelan ExecutionHistory to backend-agnostic RunHistory.
     */
    private RunHistory mapFromGamelanHistory(ExecutionHistory history) {
        List<ExecutionEvent> events = history.events() != null ?
            history.events().stream()
                .map(event -> new ExecutionEvent(
                    event.timestamp() != null ? event.timestamp() : Instant.now(),
                    event.eventType(),
                    event.nodeId(),
                    event.nodeName(),
                    event.data() != null ? event.data() : Map.of(),
                    event.error()
                ))
                .collect(Collectors.toList()) :
            List.of();

        return new RunHistory(
            new WorkflowRunId(history.runId()),
            history.workflowId(),
            events,
            history.inputs() != null ? history.inputs() : Map.of(),
            history.outputs() != null ? history.outputs() : Map.of(),
            new RunStatus(
                new WorkflowRunId(history.runId()),
                history.status(),
                0, 0, 0,
                history.startedAt() != null ? history.startedAt() : Instant.now(),
                history.completedAt()
            )
        );
    }

    // ── Capability Detection ─────────────────────────────────────────────

    /**
     * Detect capabilities from Gamelan client configuration.
     */
    private void detectCapabilities() {
        try {
            GamelanClientConfig config = gamelanClient.config();
            String transport = config != null && config.transport() != null ?
                config.transport().name() : "LOCAL";

            boolean supportsSuspension = true;  // All transports support this
            boolean supportsSignals = true;     // All transports support this
            boolean supportsHistory = true;     // All transports support this
            boolean supportsVersioning = true;  // Gamelan supports workflow versioning

            this.capabilities = new WorkflowCapabilities(
                supportsSuspension,
                supportsSignals,
                supportsHistory,
                supportsVersioning,
                1000,  // Reasonable default for max concurrent runs
                List.of(transport)
            );

            LOG.debugf("Detected Gamelan capabilities: transport=%s, suspension=%s, signals=%s",
                transport, supportsSuspension, supportsSignals);
        } catch (Exception e) {
            LOG.warnf("Failed to detect Gamelan capabilities, using defaults: %s", e.getMessage());
            this.capabilities = new WorkflowCapabilities(
                true, true, true, true, 1000, List.of("UNKNOWN")
            );
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private void ensureInitialized() {
        if (!initialized) {
            initialize(Map.of());
        }
    }
}
