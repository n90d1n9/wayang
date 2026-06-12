package tech.kayys.wayang.agent.core.core;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentClientTest {

    @Test
    void routesToRegisteredOrchestratorByStrategyId() {
        RecordingOrchestrator hermes = new RecordingOrchestrator("hermes-agent");
        AgentClient client = AgentClient.builder()
                .inferenceBackend(new RecordingInferenceBackend("fallback"))
                .orchestrator(hermes)
                .build();

        AgentResponse response = client.execute(AgentRequest.builder()
                        .prompt("remember this workflow")
                        .strategy(OrchestrationStrategy.HERMES_AGENT)
                        .build())
                .await()
                .indefinitely();

        assertThat(response.answer()).isEqualTo("handled by hermes-agent");
        assertThat(response.strategy()).isEqualTo("hermes-agent");
        assertThat(hermes.lastRequest.strategy()).isEqualTo(OrchestrationStrategy.HERMES_AGENT);
    }

    @Test
    void fallsBackToBackendBackedOrchestratorWhenNoRegistrationExists() {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("backend answer");
        AgentClient client = AgentClient.builder()
                .inferenceBackend(backend)
                .build();

        AgentResponse response = client.execute(AgentRequest.builder()
                        .prompt("plain request")
                        .strategy(OrchestrationStrategy.REACT)
                        .build())
                .await()
                .indefinitely();

        assertThat(response.answer()).isEqualTo("backend answer");
        assertThat(response.strategy()).isEqualTo("react");
        assertThat(backend.lastRequest.metadata()).containsEntry("strategy", "react");
    }

    private static final class RecordingOrchestrator implements AgentOrchestrator {
        private final String strategyId;
        private AgentRequest lastRequest;

        private RecordingOrchestrator(String strategyId) {
            this.strategyId = strategyId;
        }

        @Override
        public String strategyId() {
            return strategyId;
        }

        @Override
        public Uni<AgentResponse> execute(AgentRequest request) {
            lastRequest = request;
            return Uni.createFrom().item(AgentResponse.builder()
                    .requestId(request.requestId())
                    .answer("handled by " + strategyId)
                    .strategy(strategyId)
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
            return state.isTerminal();
        }
    }

    private static final class RecordingInferenceBackend implements InferenceBackend {
        private final String content;
        private InferenceRequest lastRequest;

        private RecordingInferenceBackend(String content) {
            this.content = content;
        }

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public String version() {
            return "test";
        }

        @Override
        public Uni<InferenceResponse> infer(InferenceRequest request) {
            lastRequest = request;
            return Uni.createFrom().item(InferenceResponse.builder()
                    .responseId("response-1")
                    .requestId(request.requestId())
                    .model(request.model())
                    .content(content)
                    .usage(InferenceTypes.TokenUsage.of(1, 1))
                    .durationMs(5)
                    .build());
        }

        @Override
        public Multi<InferenceTypes.StreamingChunk> stream(InferenceRequest request) {
            return Multi.createFrom().empty();
        }

        @Override
        public List<InferenceTypes.ProviderInfo> listProviders() {
            return List.of();
        }

        @Override
        public boolean isHealthy() {
            return true;
        }
    }
}
