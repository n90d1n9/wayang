package tech.kayys.wayang.a2a.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class A2aTaskTest {

    @Test
    void roundTripsTaskStatusArtifactsAndHistory() {
        A2aMessage userMessage = A2aMessage.user("message-1", List.of(A2aPart.text("Summarize this")));
        A2aArtifact artifact = new A2aArtifact(
                "artifact-1",
                "Summary",
                "Short summary",
                List.of(A2aPart.text("Done")),
                Map.of("quality", "draft"),
                List.of());
        A2aTask task = new A2aTask(
                "task-1",
                "context-1",
                new A2aTaskStatus(A2aTaskState.TASK_STATE_COMPLETED, null, "2026-06-02T00:00:00.000Z"),
                List.of(artifact),
                List.of(userMessage),
                Map.of("tenant", "default"));

        A2aTask decoded = A2aTask.fromJson(task.toJson());

        assertThat(decoded.id()).isEqualTo("task-1");
        assertThat(decoded.status().state()).isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(decoded.status().state().terminal()).isTrue();
        assertThat(decoded.artifacts()).hasSize(1);
        assertThat(decoded.history()).hasSize(1);
        assertThat(decoded.metadata()).containsEntry("tenant", "default");
    }

    @Test
    void roundTripsSendMessageRequestAndResponse() {
        A2aSendMessageRequest request = new A2aSendMessageRequest(
                "tenant-a",
                A2aMessage.user("message-1", List.of(A2aPart.text("Hello"))),
                new A2aSendMessageConfiguration(List.of("text/plain"), Map.of("url", "https://hook.test"), 0, true),
                Map.of("requestId", "req-1"));

        A2aSendMessageRequest decoded = A2aSendMessageRequest.fromJson(request.toJson());

        assertThat(decoded.tenant()).isEqualTo("tenant-a");
        assertThat(decoded.configuration().acceptedOutputModes()).containsExactly("text/plain");
        assertThat(decoded.configuration().historyLength()).isZero();
        assertThat(decoded.configuration().returnImmediately()).isTrue();

        A2aSendMessageResponse response = A2aSendMessageResponse.message(
                A2aMessage.agent("message-2", List.of(A2aPart.text("Hi"))));

        assertThat(A2aSendMessageResponse.fromMap(response.toMap()).message().role())
                .isEqualTo(A2aRole.ROLE_AGENT);
    }

    @Test
    void validatesOneOfResponseEnvelopes() {
        A2aMessage message = A2aMessage.agent("message-1", List.of(A2aPart.text("Hi")));
        A2aTask task = new A2aTask("task-1", null, A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(), List.of(), Map.of());

        assertThatThrownBy(() -> new A2aSendMessageResponse(task, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
        assertThatThrownBy(() -> new A2aStreamResponse(task, message, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }
}
