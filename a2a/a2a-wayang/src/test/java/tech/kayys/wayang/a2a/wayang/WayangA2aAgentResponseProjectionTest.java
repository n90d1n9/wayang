package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aAgentResponseProjectionTest {

    @Test
    void resolvesMessageIdFromRunIdThenRequestIdFallback() {
        assertThat(WayangA2aAgentResponseProjection.messageId(AgentResponse.builder()
                .runId("run-1")
                .requestId("request-1")
                .build()))
                .isEqualTo("run-1");
        assertThat(WayangA2aAgentResponseProjection.messageId(AgentResponse.builder()
                .requestId("request-1")
                .build()))
                .isEqualTo("request-1-response");
        assertThat(WayangA2aAgentResponseProjection.messageId(AgentResponse.builder().build()))
                .isEqualTo("wayang-response");
    }

    @Test
    void resolvesTextFromAnswerThenErrorFallback() {
        assertThat(WayangA2aAgentResponseProjection.text(AgentResponse.builder()
                .answer("done")
                .error("boom")
                .build()))
                .isEqualTo("done");
        assertThat(WayangA2aAgentResponseProjection.text(AgentResponse.builder()
                .error("boom")
                .build()))
                .isEqualTo("boom");
        assertThat(WayangA2aAgentResponseProjection.text(AgentResponse.builder().build()))
                .isEmpty();
    }

    @Test
    void projectsStableMetadataFields() {
        Map<String, Object> metadata = WayangA2aAgentResponseProjection.metadata(AgentResponse.builder()
                .runId("run-1")
                .requestId("request-1")
                .successful(false)
                .error("boom")
                .strategy("react")
                .totalSteps(3)
                .durationMs(42)
                .build());

        assertThat(metadata)
                .containsEntry("runId", "run-1")
                .containsEntry("requestId", "request-1")
                .containsEntry("successful", false)
                .containsEntry("totalSteps", 3)
                .containsEntry("durationMs", 42L)
                .containsEntry("strategy", "react")
                .containsEntry("error", "boom");
        assertThat(metadata.keySet()).containsExactly(
                "runId",
                "requestId",
                "successful",
                "totalSteps",
                "durationMs",
                "strategy",
                "error");
    }
}
