package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Hermes mode decorator over any existing Wayang orchestrator.
 */
public final class HermesAgentOrchestrator implements AgentOrchestrator {

    private final AgentOrchestrator delegate;
    private final HermesAgentModeConfig config;
    private final HermesRuntimeCapabilities capabilities;
    private final HermesPromptAssembler promptAssembler;
    private final HermesRequestPlanner requestPlanner;
    private final HermesDirectiveDispatcher directiveDispatcher;
    private final HermesMemorySnapshotProvider memorySnapshotProvider;
    private final HermesLearningLoop learningLoop;
    private final HermesRuntimeEventSink runtimeEventSink;
    private final HermesRuntimeDiagnostics runtimeDiagnostics;
    private final HermesLearningAuditRetentionObserver retentionObserver;

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesLearningLoop learningLoop) {
        this(delegate, HermesAgentModeConfig.defaults(), HermesMemorySnapshotProvider.none(), learningLoop);
    }

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesAgentModeConfig config,
            HermesMemorySnapshotProvider memorySnapshotProvider,
            HermesLearningLoop learningLoop) {
        this(delegate, config, memorySnapshotProvider, learningLoop, HermesRuntimePorts.noop());
    }

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesAgentModeConfig config,
            HermesMemorySnapshotProvider memorySnapshotProvider,
            HermesLearningLoop learningLoop,
            HermesRuntimePorts runtimePorts) {
        this(delegate, config, memorySnapshotProvider, learningLoop, runtimePorts, HermesRuntimeEventSink.noop());
    }

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesAgentModeConfig config,
            HermesMemorySnapshotProvider memorySnapshotProvider,
            HermesLearningLoop learningLoop,
            HermesRuntimePorts runtimePorts,
            HermesRuntimeEventSink runtimeEventSink) {
        this(delegate, config, memorySnapshotProvider, learningLoop, runtimePorts, runtimeEventSink, null);
    }

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesAgentModeConfig config,
            HermesMemorySnapshotProvider memorySnapshotProvider,
            HermesLearningLoop learningLoop,
            HermesRuntimePorts runtimePorts,
            HermesRuntimeEventSink runtimeEventSink,
            HermesRuntimeDiagnostics runtimeDiagnostics) {
        this(
                delegate,
                config,
                memorySnapshotProvider,
                learningLoop,
                runtimePorts,
                runtimeEventSink,
                runtimeDiagnostics,
                HermesLearningAuditRetentionObserver.noop());
    }

    public HermesAgentOrchestrator(
            AgentOrchestrator delegate,
            HermesAgentModeConfig config,
            HermesMemorySnapshotProvider memorySnapshotProvider,
            HermesLearningLoop learningLoop,
            HermesRuntimePorts runtimePorts,
            HermesRuntimeEventSink runtimeEventSink,
            HermesRuntimeDiagnostics runtimeDiagnostics,
            HermesLearningAuditRetentionObserver retentionObserver) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
        this.capabilities = this.config.runtimeCapabilities();
        this.promptAssembler = new HermesPromptAssembler(this.config);
        this.requestPlanner = new HermesRequestPlanner(this.config);
        HermesRuntimePorts effectiveRuntimePorts = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        HermesRuntimeDiagnostics effectiveRuntimeDiagnostics = runtimeDiagnostics == null
                ? HermesRuntimeDiagnostics.from(this.config, effectiveRuntimePorts)
                : runtimeDiagnostics;
        this.directiveDispatcher = new HermesDirectiveDispatcher(
                effectiveRuntimePorts,
                HermesRuntimeDiagnosticsPort.service(effectiveRuntimeDiagnostics));
        this.memorySnapshotProvider = memorySnapshotProvider == null
                ? HermesMemorySnapshotProvider.none()
                : memorySnapshotProvider;
        this.learningLoop = Objects.requireNonNull(learningLoop, "learningLoop");
        this.runtimeEventSink = runtimeEventSink == null ? HermesRuntimeEventSink.noop() : runtimeEventSink;
        this.runtimeDiagnostics = effectiveRuntimeDiagnostics;
        this.retentionObserver = retentionObserver == null
                ? HermesLearningAuditRetentionObserver.noop()
                : retentionObserver;
    }

    @Override
    public String strategyId() {
        return HermesAgentMode.MODE_ID;
    }

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        long started = System.nanoTime();
        return enrich(request)
                .flatMap(enriched -> delegate.execute(enriched)
                        .flatMap(response -> {
                            AgentResponse hermesResponse = withHermesStrategy(response, elapsedMs(started));
                            emit(HermesRuntimeEvent.responseCompleted(enriched, hermesResponse));
                            return learningLoop.learn(enriched, response)
                                    .invoke(result -> {
                                        emit(HermesRuntimeEvent.skillLearningCompleted(
                                                enriched,
                                                hermesResponse,
                                                result));
                                        retentionObserver.observe();
                                    })
                                    .replaceWith(hermesResponse)
                                    .onFailure().invoke(error -> emit(HermesRuntimeEvent.skillLearningFailed(
                                            enriched,
                                            hermesResponse,
                                            error)))
                                    .onFailure().recoverWithItem(hermesResponse);
                        }))
                .onFailure().invoke(error -> emit(HermesRuntimeEvent.responseFailed(request, error, elapsedMs(started))));
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().uni(enrich(request))
                .flatMap(delegate::stream);
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        return delegate.step(state);
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return delegate.isTerminal(state);
    }

    @Override
    public String getSystemPromptFragment() {
        return promptAssembler.baseSystemPrompt();
    }

    @Override
    public List<String> supportedFeatures() {
        return capabilities.enabledFeatures();
    }

    @Override
    public boolean supportsToolCalling() {
        return capabilities.requiresToolCalling() || delegate.supportsToolCalling();
    }

    @Override
    public boolean supportsStreaming() {
        return delegate.supportsStreaming();
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }

    @Override
    public boolean supportsMultiAgent() {
        return capabilities.supportsSubAgents() || delegate.supportsMultiAgent();
    }

    @Override
    public Optional<String> getPreferredProvider() {
        if ("auto".equalsIgnoreCase(config.preferredProvider())) {
            return delegate.getPreferredProvider();
        }
        return Optional.of(config.preferredProvider());
    }

    @Override
    public Map<String, Object> getRecommendedParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>(delegate.getRecommendedParameters());
        parameters.put(HermesMetadataKeys.METADATA_TEMPERATURE, 0.35);
        parameters.put(HermesMetadataKeys.METADATA_TOOL_CHOICE, capabilities.requiresToolCalling() ? "auto" : "none");
        parameters.put(HermesMetadataKeys.METADATA_MODE, HermesAgentMode.MODE_ID);
        parameters.put(HermesMetadataKeys.METADATA_PREFER_LOCAL, capabilities.prefersLocalProviders());
        parameters.put(HermesMetadataKeys.METADATA_CAPABILITIES, capabilities.toMetadata());
        parameters.put(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS, runtimeDiagnosticsMetadata());
        parameters.put(HermesMetadataKeys.PARAM_METADATA_CONTRACT, HermesMetadataContract.current().toMetadata());
        parameters.putAll(requestPlanner.defaultPlan().parameterMetadata());
        return Map.copyOf(parameters);
    }

    @Override
    public Duration getStepTimeout() {
        return delegate.getStepTimeout();
    }

    @Override
    public int getMaxSteps() {
        return Math.max(delegate.getMaxSteps(), 15);
    }

    @Override
    public List<InferenceTypes.ToolDefinition> getToolDefinitions(AgentRequest request) {
        return delegate.getToolDefinitions(request);
    }

    @Override
    public InferenceRequest.Builder buildInferenceRequest(AgentRequest request, AgentState state) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(HermesMetadataKeys.METADATA_MODE, HermesAgentMode.MODE_ID);
        metadata.put(HermesMetadataKeys.METADATA_FEATURES, capabilities.enabledFeatures());
        metadata.put(HermesMetadataKeys.METADATA_CONFIG, config.toMetadata());
        metadata.put(HermesMetadataKeys.METADATA_CAPABILITIES, capabilities.toMetadata());
        metadata.put(HermesMetadataKeys.METADATA_CONTRACT, HermesMetadataContract.current().toMetadata());
        metadata.put(HermesMetadataKeys.METADATA_RUNTIME_DIAGNOSTICS, runtimeDiagnosticsMetadata());
        metadata.putAll(requestPlanner.plan(request).parameterMetadata());
        Object dispatchReport = request.parameters().get(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT);
        if (dispatchReport != null) {
            metadata.put(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT, dispatchReport);
        }
        return delegate.buildInferenceRequest(request, state)
                .metadata(metadata);
    }

    private Uni<AgentRequest> enrich(AgentRequest request) {
        HermesRequestPlan requestPlan = requestPlanner.plan(request);
        emit(HermesRuntimeEvent.requestPlanned(request, requestPlan));
        HermesDirectiveDispatchReport dispatchReport = directiveDispatcher.dispatch(requestPlan);
        emit(HermesRuntimeEvent.directivesDispatched(request, dispatchReport));
        return memorySnapshotProvider.snapshot(request)
                .map(snapshot -> withRuntimeDiagnostics(
                        promptAssembler.enrich(request, snapshot, requestPlan, dispatchReport)));
    }

    public HermesRuntimeDiagnostics runtimeDiagnostics() {
        return runtimeDiagnostics;
    }

    public HermesLearningAuditRetentionObservation learningAuditRetentionObservation() {
        return retentionObserver.lastObservation();
    }

    public HermesPortDispatchResult inspectRuntimeDiagnostics(HermesRuntimeDiagnosticsDirective directive) {
        return withRetentionObservation(directiveDispatcher.dispatchRuntimeDiagnostics(directive));
    }

    public HermesPortDispatchResult inspectLearningAudit(HermesLearningAuditDirective directive) {
        return directiveDispatcher.dispatchLearningAudit(directive);
    }

    private AgentRequest withRuntimeDiagnostics(AgentRequest request) {
        Map<String, Object> diagnosticsMetadata = runtimeDiagnosticsMetadata();
        Map<String, Object> context = new LinkedHashMap<>(request.context());
        context.put(HermesMetadataKeys.CONTEXT_RUNTIME_DIAGNOSTICS, diagnosticsMetadata);
        Map<String, Object> parameters = new LinkedHashMap<>(request.parameters());
        parameters.put(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS, diagnosticsMetadata);
        return new AgentRequest(
                request.requestId(),
                request.prompt(),
                request.systemPrompt(),
                request.strategy(),
                request.allowedSkills(),
                context,
                parameters,
                request.tenantId(),
                request.sessionId(),
                request.userId(),
                request.stream(),
                request.verbose(),
                request.timeout(),
                request.memoryConfig(),
                request.modelId(),
                request.timestamp());
    }

    private Map<String, Object> runtimeDiagnosticsMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>(runtimeDiagnostics.toMetadata());
        metadata.put(
                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                retentionObserver.toMetadata());
        metadata.computeIfPresent("learningAudit", (ignored, value) ->
                withNestedRetentionObservation(value));
        return Map.copyOf(metadata);
    }

    private HermesPortDispatchResult withRetentionObservation(HermesPortDispatchResult result) {
        if (result == null) {
            return null;
        }
        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        Map<String, Object> observation = retentionObserver.toMetadata();
        metadata.put(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION, observation);
        metadata.computeIfPresent("diagnostics", (ignored, value) ->
                withNestedRetentionObservation(value));
        return new HermesPortDispatchResult(
                result.port(),
                result.operation(),
                result.target(),
                result.active(),
                result.dispatched(),
                result.successful(),
                result.status(),
                result.reason(),
                metadata);
    }

    private Map<String, Object> withNestedRetentionObservation(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of(
                    "value", value,
                    HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                    retentionObserver.toMetadata());
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> {
            if (key != null) {
                metadata.put(String.valueOf(key), mapValue);
            }
        });
        metadata.put(
                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                retentionObserver.toMetadata());
        return Map.copyOf(metadata);
    }

    private void emit(HermesRuntimeEvent event) {
        try {
            runtimeEventSink.emit(event);
        } catch (RuntimeException ignored) {
            // Runtime event persistence must not break the agent execution path.
        }
    }

    private static AgentResponse withHermesStrategy(AgentResponse response, long durationMs) {
        return new AgentResponse(
                response.runId(),
                response.requestId(),
                response.answer(),
                response.steps(),
                response.totalSteps(),
                response.successful(),
                response.error(),
                HermesAgentMode.MODE_ID,
                durationMs > 0 ? durationMs : response.durationMs(),
                response.completedAt());
    }

    private static long elapsedMs(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
