package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunResponseMapperTest {

    private final AgentRunResponseMapper mapper = new AgentRunResponseMapper();

    @Test
    void mapsAgentResponseToStableApiPayload() {
        Response response = mapper.ok(response("run-1", "request-1", "done", true, null));

        assertThat(response.getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertThat(body)
                .containsEntry("runId", "run-1")
                .containsEntry("requestId", "request-1")
                .containsEntry("answer", "done")
                .containsEntry("totalSteps", 3)
                .containsEntry("successful", true)
                .containsEntry("strategy", "react")
                .containsEntry("durationMs", 42L)
                .containsEntry("error", null);
    }

    @Test
    void mapsBadRequestAndServerErrorsWithSafeMessages() {
        assertThat(mapper.badRequest(new IllegalArgumentException("Prompt is required")))
                .returns(400, Response::getStatus)
                .extracting(Response::getEntity)
                .isEqualTo(Map.of("error", "Prompt is required"));

        assertThat(mapper.serverError(new RuntimeException()))
                .returns(500, Response::getStatus)
                .extracting(Response::getEntity)
                .isEqualTo(Map.of("error", "RuntimeException"));
    }

    @Test
    void formatsStreamingDataAndErrors() {
        assertThat(mapper.streamData(response("run-1", "request-1", "chunk", true, null)))
                .isEqualTo("data: chunk\n\n");
        assertThat(mapper.streamData(response("run-1", "request-1", null, true, null)))
                .isEqualTo("data: \n\n");
        assertThat(mapper.streamError(null))
                .isEqualTo("event: error\ndata: Unknown error\n\n");
    }

    private AgentResponse response(String runId, String requestId, String answer, boolean successful, String error) {
        return AgentResponse.builder()
                .runId(runId)
                .requestId(requestId)
                .answer(answer)
                .totalSteps(3)
                .successful(successful)
                .error(error)
                .strategy("react")
                .durationMs(42)
                .build();
    }
}
