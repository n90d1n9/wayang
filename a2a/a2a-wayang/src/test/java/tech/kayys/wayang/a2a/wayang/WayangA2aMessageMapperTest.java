package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aSendMessageResponse;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aMessageMapperTest {

    private final WayangA2aMessageMapper mapper = new WayangA2aMessageMapper();

    @Test
    void mapsA2aSendMessageRequestToAgentRequest() {
        A2aMessage message = new A2aMessage(
                "message-1",
                "context-1",
                "task-1",
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("First"), A2aPart.text("Second")),
                Map.of("allowedSkills", List.of("chat", "rag")),
                List.of("https://example.test/ext"),
                List.of());
        A2aSendMessageRequest request = new A2aSendMessageRequest(
                "tenant-a",
                message,
                A2aSendMessageConfiguration.textOutput(),
                Map.of());

        AgentRequest mapped = mapper.toAgentRequest(request, true);

        assertThat(mapped.requestId()).isEqualTo("message-1");
        assertThat(mapped.prompt()).isEqualTo("First\nSecond");
        assertThat(mapped.tenantId()).isEqualTo("tenant-a");
        assertThat(mapped.sessionId()).isEqualTo("context-1");
        assertThat(mapped.stream()).isTrue();
        assertThat(mapped.allowedSkills()).containsExactly("chat", "rag");
        assertThat(mapped.context()).containsKey(WayangA2a.CONTEXT_KEY);
        @SuppressWarnings("unchecked")
        Map<String, Object> a2aContext = (Map<String, Object>) mapped.context().get(WayangA2a.CONTEXT_KEY);
        assertThat(a2aContext)
                .containsEntry(WayangA2a.MESSAGE_ID_KEY, "message-1")
                .containsEntry(WayangA2a.CONTEXT_ID_KEY, "context-1");
        assertThat(a2aContext.keySet()).containsExactly(
                WayangA2a.MESSAGE_ID_KEY,
                WayangA2a.CONTEXT_ID_KEY,
                WayangA2a.TASK_ID_KEY,
                WayangA2a.TENANT_KEY,
                WayangA2a.EXTENSIONS_KEY,
                WayangA2a.METADATA_KEY,
                WayangA2a.CONFIGURATION_KEY,
                WayangA2a.PARTS_KEY);
    }

    @Test
    void mapsSuccessfulAgentResponseToCompletedTask() {
        AgentResponse response = AgentResponse.builder()
                .runId("run-1")
                .requestId("message-1")
                .answer("Done")
                .strategy("react")
                .totalSteps(3)
                .durationMs(42)
                .build();

        A2aSendMessageResponse mapped = mapper.toSendMessageResponse("task-1", "context-1", response);

        A2aTask task = mapped.task();
        assertThat(task.status().state()).isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(task.artifacts()).singleElement()
                .satisfies(artifact -> assertThat(artifact.parts()).singleElement()
                        .extracting(A2aPart::text)
                        .isEqualTo("Done"));
        assertThat(task.metadata()).containsEntry("runId", "run-1");
    }

    @Test
    void mapsFailedAgentResponseToFailedTaskWithStatusMessage() {
        AgentResponse response = AgentResponse.builder()
                .runId("run-2")
                .requestId("message-2")
                .successful(false)
                .error("boom")
                .build();

        A2aTask task = mapper.toTask("task-2", "context-2", response);

        assertThat(task.status().state()).isEqualTo(A2aTaskState.TASK_STATE_FAILED);
        assertThat(task.status().message().parts()).singleElement()
                .extracting(A2aPart::text)
                .isEqualTo("boom");
        assertThat(task.artifacts()).isEmpty();
    }
}
