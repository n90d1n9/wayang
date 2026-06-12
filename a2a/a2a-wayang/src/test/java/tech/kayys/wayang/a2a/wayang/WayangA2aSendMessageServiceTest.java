package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageConfiguration;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSendMessageServiceTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void executesSendMessageAndRecordsTaskLifecycle() {
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(store, request -> {
            capturedPrompt.set(request.prompt());
            return AgentResponse.builder()
                    .runId("run-1")
                    .requestId(request.requestId())
                    .answer("pong")
                    .strategy("react")
                    .totalSteps(2)
                    .durationMs(12)
                    .build();
        });

        WayangA2aSendMessageResult result = service.send(request("message-1", "context-1", "task-1", "ping"));
        Map<String, Object> resultMap = result.toMap();
        String resultJson = WayangA2aHttpJson.write(resultMap);

        A2aTask task = result.task();
        assertThat(capturedPrompt).hasValue("ping");
        assertThat(task.id()).isEqualTo("task-1");
        assertThat(task.contextId()).isEqualTo("context-1");
        assertThat(task.status().state()).isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(task.history()).hasSize(2);
        assertThat(task.artifacts()).singleElement()
                .satisfies(artifact -> assertThat(artifact.parts()).singleElement()
                        .extracting(A2aPart::text)
                        .isEqualTo("pong"));
        assertThat(store.events("task-1", 0, 20))
                .extracting(WayangA2aTaskEvent::type)
                .containsExactly(
                        WayangA2aTaskEvent.TYPE_TASK_CREATED,
                        WayangA2aTaskEvent.TYPE_MESSAGE_APPENDED,
                        WayangA2aTaskEvent.TYPE_STATUS_UPDATED,
                        WayangA2aTaskEvent.TYPE_MESSAGE_APPENDED,
                        WayangA2aTaskEvent.TYPE_ARTIFACT_APPENDED,
                        WayangA2aTaskEvent.TYPE_STATUS_UPDATED);
        assertThat(task.metadata().keySet()).containsExactly("messageId", "tenant");
        assertThat(resultMap.keySet()).containsExactly("task", "agentRequestId", "agentRunId", "successful");
        assertThat(resultJson).startsWith("{\"task\":");
        assertThat(resultJson).contains("\"agentRequestId\":\"message-1\"");
        assertThat(resultJson).contains("\"agentRunId\":\"run-1\"");
    }

    @Test
    void storesPushNotificationConfigAndProjectsResponseHistory() {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                store,
                request -> AgentResponse.builder()
                        .runId("run-config")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest configuredRequest = request(
                "message-config",
                "context-config",
                "task-config",
                "ping",
                new A2aSendMessageConfiguration(
                        List.of("text/plain"),
                        Map.of(
                                "configId", "primary",
                                "url", "https://hooks.test/a2a"),
                        1,
                        null));

        WayangA2aSendMessageResult result = service.send(configuredRequest);

        assertThat(result.task().history()).hasSize(2);
        assertThat(result.responseTask().history())
                .hasSize(1)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.role()).isEqualTo(A2aRole.ROLE_AGENT);
                    assertThat(message.parts()).singleElement()
                            .extracting(A2aPart::text)
                            .isEqualTo("pong");
                });
        assertThat(store.get("task-config")).contains(result.task());
        assertThat(store.getPushNotificationConfig("task-config", "primary"))
                .get()
                .satisfies(config -> assertThat(config.url()).isEqualTo("https://hooks.test/a2a"));
    }

    @Test
    void capturesExecutorFailuresAsFailedTasks() {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                store,
                request -> {
                    throw new IllegalStateException("boom");
                });

        WayangA2aSendMessageResult result = service.send(request("message-2", "context-2", "task-2", "ping"));

        assertThat(result.agentResponse().successful()).isFalse();
        assertThat(result.task().status().state()).isEqualTo(A2aTaskState.TASK_STATE_FAILED);
        assertThat(result.task().status().message().parts()).singleElement()
                .extracting(A2aPart::text)
                .isEqualTo("boom");
        assertThat(store.get("task-2")).contains(result.task());
    }

    @Test
    void resolvesTaskAndContextIdsFromMetadataWhenMessageOmitsThem() {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                store,
                request -> AgentResponse.builder()
                        .runId("run-metadata")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build());
        A2aSendMessageRequest request = new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        "message-metadata",
                        null,
                        null,
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                Map.of("taskId", "task-metadata", "contextId", "context-metadata"));

        WayangA2aSendMessageResult result = service.send(request);

        assertThat(result.task().id()).isEqualTo("task-metadata");
        assertThat(result.task().contextId()).isEqualTo("context-metadata");
        assertThat(store.get("task-metadata")).contains(result.task());
    }

    static A2aSendMessageRequest request(String messageId, String contextId, String taskId, String text) {
        return request(messageId, contextId, taskId, text, null);
    }

    static A2aSendMessageRequest request(
            String messageId,
            String contextId,
            String taskId,
            String text,
            A2aSendMessageConfiguration configuration) {
        return new A2aSendMessageRequest(
                "tenant-a",
                new A2aMessage(
                        messageId,
                        contextId,
                        taskId,
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text(text)),
                        Map.of(),
                        List.of(),
                        List.of()),
                configuration,
                Map.of());
    }
}
