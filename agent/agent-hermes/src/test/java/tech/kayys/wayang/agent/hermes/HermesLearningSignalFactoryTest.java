package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.AgentState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningSignalFactoryTest {

    @Test
    void extractsToolIdsAndMergedRunMetadata() {
        AgentRequest request = AgentRequest.builder()
                .requestId("req-signal")
                .prompt("Create release summary")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .modelId("model-a")
                .context(Map.of("channel", "telegram"))
                .parameter(HermesAgentMode.PARAM_LEARN_KEY, "yes")
                .build();
        AgentResponse response = AgentResponse.builder()
                .runId("run-signal")
                .requestId("req-signal")
                .answer("Summary ready")
                .steps(List.of(step(1, "rag"), step(2, "rag"), step(3, "terminal")))
                .successful(true)
                .strategy("react")
                .build();

        HermesLearningSignal signal = new HermesLearningSignalFactory().from(request, response);

        assertThat(signal.requestId()).isEqualTo("req-signal");
        assertThat(signal.task()).isEqualTo("Create release summary");
        assertThat(signal.answer()).isEqualTo("Summary ready");
        assertThat(signal.toolIds()).containsExactly("rag", "terminal");
        assertThat(signal.metadata())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("modelId", "model-a")
                .containsEntry("strategy", "react")
                .containsEntry("channel", "telegram")
                .containsEntry(HermesAgentMode.PARAM_LEARN_KEY, "yes");
    }

    private static AgentState.ReasoningStep step(int number, String skillId) {
        return new AgentState.ReasoningStep(
                number,
                "Step " + number,
                new AgentState.AgentAction(skillId, "complete step " + number, Map.of(), Instant.now()),
                "ok",
                5,
                true);
    }
}
