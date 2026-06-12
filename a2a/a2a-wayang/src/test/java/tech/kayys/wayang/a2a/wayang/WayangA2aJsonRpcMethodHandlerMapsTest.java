package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcMethodHandlerMapsTest {

    @Test
    void builderPreservesOrderAndAllowsLaterHandlersToReplaceExistingMethods() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler overrideHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler secondHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(202, "{}");
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> extra = new LinkedHashMap<>();
        extra.put(WayangA2aJsonRpcMethods.SEND_MESSAGE, overrideHandler);
        extra.put(WayangA2aJsonRpcMethods.GET_TASK, secondHandler);

        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers =
                WayangA2aJsonRpcMethodHandlerMaps.builder()
                        .put(WayangA2aJsonRpcMethods.SEND_MESSAGE, firstHandler)
                        .putAll(extra)
                        .build();

        assertThat(handlers.keySet()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(handlers.get(WayangA2aJsonRpcMethods.SEND_MESSAGE)).isSameAs(overrideHandler);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> handlers.put(WayangA2aJsonRpcMethods.LIST_TASKS, secondHandler));
    }

    @Test
    void builderRejectsBlankMethodKeys() {
        WayangA2aJsonRpcMethodDispatchTable.Handler handler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");

        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(" ", handler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("method must not be blank");
    }
}
