package tech.kayys.wayang.agent.core.core;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal orchestrator that delegates a single agent request to an inference
 * backend. Feature modules can use this as a default delegate when they only
 * need to decorate prompt assembly, memory, learning, or response handling.
 */
public final class BackendBackedAgentOrchestrator implements AgentOrchestrator {

    private final String strategyId;
    private final InferenceBackend inferenceBackend;

    public BackendBackedAgentOrchestrator(String strategyId, InferenceBackend inferenceBackend) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("strategyId is required");
        }
        this.strategyId = strategyId;
        this.inferenceBackend = Objects.requireNonNull(inferenceBackend, "inferenceBackend");
    }

    @Override
    public String strategyId() {
        return strategyId;
    }

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        InferenceRequest inferenceRequest = InferenceRequest.builder()
                .requestId(request.requestId())
                .model(request.modelId())
                .message(new InferenceTypes.UserMessage(request.prompt()))
                .stream(request.stream())
                .timeout(request.getTimeout())
                .metadata(Map.of("strategy", strategyId))
                .build();

        return inferenceBackend.infer(inferenceRequest)
                .map(response -> AgentResponse.builder()
                        .requestId(request.requestId())
                        .answer(response.message() != null ? response.message().content() : "")
                        .strategy(strategyId)
                        .durationMs(response.durationMs())
                        .successful(true)
                        .build());
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().empty();
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        return Uni.createFrom().item(state);
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return true;
    }

    @Override
    public List<String> supportedFeatures() {
        return List.of("backend-inference");
    }
}
