package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSendMessageHttpHandlersTest {

    @Test
    void dispatchesSendMessageAndThenReadsStoredTask() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card(),
                WayangA2aHttpHandlers.forExecution(store, request -> AgentResponse.builder()
                        .runId("run-1")
                        .requestId(request.requestId())
                        .answer("pong")
                        .strategy("react")
                        .build()));

        WayangA2aHttpResponse send = dispatcher.dispatch(WayangA2aHttpRequest.sendMessage(
                WayangA2aSendMessageServiceTest.request("message-1", "context-1", "task-1", "ping").toJson()));
        WayangA2aHttpResponse get = dispatcher.dispatch(WayangA2aHttpRequest.get("/tasks/task-1"));

        assertThat(send.statusCode()).isEqualTo(200);
        assertThat(send.headers()).containsEntry(
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                A2aProtocol.OPERATION_SEND_MESSAGE);
        A2aTask sentTask = A2aTask.fromMap((Map<?, ?>) WayangA2aHttpJson.read(send.body()).get("task"));
        assertThat(sentTask.status().state()).isEqualTo(A2aTaskState.TASK_STATE_COMPLETED);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(A2aTask.fromJson(get.body()).artifacts()).hasSize(1);
    }

    @Test
    void dispatchesStreamingSendMessageAsEventStreamAndStoresTask() {
        InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                store,
                request -> AgentResponse.builder()
                        .runId("run-2")
                        .requestId(request.requestId())
                        .answer("streamed")
                        .strategy("react")
                        .build());
        WayangA2aHttpOperationDispatcher dispatcher = new WayangA2aHttpOperationDispatcher(
                card(),
                WayangA2aHttpHandlers.merge(
                        WayangA2aSendMessageHttpHandlers.forService(service),
                        WayangA2aTaskHttpHandlers.forStore(store)));

        WayangA2aHttpResponse stream = dispatcher.dispatch(WayangA2aHttpRequest.streamMessage(
                WayangA2aSendMessageServiceTest.request("message-2", "context-2", "task-2", "ping").toJson()));

        assertThat(stream.statusCode()).isEqualTo(200);
        assertThat(stream.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(stream.body()).contains("\"task\"");
        assertThat(store.get("task-2")).isPresent();
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                new A2aAgentCapabilities(true, false, List.of(), false),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                List.of(),
                null);
    }
}
