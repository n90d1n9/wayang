package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aHttpHandlerMapsTest {

    @Test
    void mergePreservesHandlerOrderAndTrimsOperationKeys() {
        WayangA2aHttpOperationHandler firstHandler = (request, match) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aHttpOperationHandler overrideHandler = (request, match) -> WayangA2aHttpResponse.json(201, "{}");
        WayangA2aHttpOperationHandler secondHandler = (request, match) -> WayangA2aHttpResponse.json(202, "{}");
        Map<String, WayangA2aHttpOperationHandler> first = new LinkedHashMap<>();
        first.put(" " + A2aProtocol.OPERATION_SEND_MESSAGE + " ", firstHandler);
        first.put(null, secondHandler);
        Map<String, WayangA2aHttpOperationHandler> second = new LinkedHashMap<>();
        second.put(A2aProtocol.OPERATION_SEND_MESSAGE, overrideHandler);
        second.put(A2aProtocol.OPERATION_GET_TASK, secondHandler);

        Map<String, WayangA2aHttpOperationHandler> merged = WayangA2aHttpHandlers.merge(first, second);

        assertThat(merged.keySet()).containsExactly(
                A2aProtocol.OPERATION_SEND_MESSAGE,
                A2aProtocol.OPERATION_GET_TASK);
        assertThat(merged.get(A2aProtocol.OPERATION_SEND_MESSAGE)).isSameAs(overrideHandler);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> merged.put(A2aProtocol.OPERATION_LIST_TASKS, secondHandler));
    }

    @Test
    void strictCopyRejectsBlankOperationKeys() {
        WayangA2aHttpOperationHandler handler = (request, match) -> WayangA2aHttpResponse.json(200, "{}");

        assertThatThrownBy(() -> WayangA2aHttpHandlerMaps.copyStrict(Map.of("", handler)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operation handler key");
    }

    @Test
    void handlerFactoriesKeepCanonicalOperationOrder() {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                new InMemoryWayangA2aTaskStore(),
                request -> null);

        assertThat(WayangA2aSendMessageHttpHandlers.forService(service).keySet())
                .containsExactly(
                        A2aProtocol.OPERATION_SEND_MESSAGE,
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE);
        assertThat(WayangA2aTaskHttpHandlers.forStore(new InMemoryWayangA2aTaskStore()).keySet())
                .containsExactly(
                        A2aProtocol.OPERATION_GET_TASK,
                        A2aProtocol.OPERATION_LIST_TASKS,
                        A2aProtocol.OPERATION_CANCEL_TASK,
                        A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK,
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
    }
}
