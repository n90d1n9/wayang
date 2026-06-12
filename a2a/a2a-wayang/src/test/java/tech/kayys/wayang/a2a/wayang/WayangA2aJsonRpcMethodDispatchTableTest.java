package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcMethodDispatchTableTest {

    @Test
    void completeTableFollowsRegisteredMethodOrder() {
        WayangA2aJsonRpcMethodDispatchTable table =
                WayangA2aJsonRpcMethodDispatchTable.requireComplete(handlersForAllMethods());

        assertThat(table.complete()).isTrue();
        assertThat(table.methodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(table.methods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(table.methodGroups())
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_SEND, List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY, List.of(
                        WayangA2aJsonRpcMethods.GET_TASK,
                        WayangA2aJsonRpcMethods.LIST_TASKS));
        assertThat(table.missingRegisteredMethods()).isEmpty();
        assertThat(table.coverage().complete()).isTrue();
        assertThat(table.entry(WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .get()
                .satisfies(entry -> assertThat(entry)
                        .returns(WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethodDispatchTable.Entry::method)
                        .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_SEND,
                                WayangA2aJsonRpcMethodDispatchTable.Entry::methodGroup)
                        .returns(false, WayangA2aJsonRpcMethodDispatchTable.Entry::streaming));
        assertThat(table.entry(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))
                .get()
                .satisfies(entry -> assertThat(entry.streaming()).isTrue());
    }

    @Test
    void reportsMissingRegisteredMethodsForPartialTables() {
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers = handlersForAllMethods();
        handlers.remove(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);

        WayangA2aJsonRpcMethodDispatchTable table = WayangA2aJsonRpcMethodDispatchTable.of(handlers);

        assertThat(table.complete()).isFalse();
        assertThat(table.supports(WayangA2aJsonRpcMethods.SEND_MESSAGE)).isTrue();
        assertThat(table.supports(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD)).isFalse();
        assertThat(table.missingRegisteredMethods())
                .containsExactly(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
        assertThat(table.coverage().complete()).isFalse();
        assertThat(table.coverage().missingDispatchMethods())
                .containsExactly(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
        assertThat(table.methodGroups())
                .doesNotContainKey(WayangA2aJsonRpcMethods.METHOD_GROUP_AGENT_CARD);
        assertThatThrownBy(() -> WayangA2aJsonRpcMethodDispatchTable.requireComplete(handlers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
    }

    @Test
    void dispatchCoverageUsesTableMethodGroupMetadata() {
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers = handlersForAllMethods();
        handlers.remove(WayangA2aJsonRpcMethods.GET_TASK);

        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchTable.of(handlers).coverage();

        assertThat(coverage.methodGroups())
                .anySatisfy(group -> assertThat(group)
                        .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::group)
                        .returns(List.of(WayangA2aJsonRpcMethods.GET_TASK),
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::missingDispatchMethods)
                        .returns(List.of(WayangA2aJsonRpcMethods.LIST_TASKS),
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::dispatchMethods));
    }

    @Test
    void rejectsHandlersForUnknownMethods() {
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers = handlersForAllMethods();
        handlers.put("UnknownMethod", okHandler("unknown"));

        assertThatThrownBy(() -> WayangA2aJsonRpcMethodDispatchTable.of(handlers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JSON-RPC method: UnknownMethod");
    }

    @Test
    void dispatchesThroughRegisteredHandler() {
        WayangA2aJsonRpcMethodDispatchTable table =
                WayangA2aJsonRpcMethodDispatchTable.requireComplete(handlersForAllMethods());
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "dispatch-1",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of());

        WayangA2aHttpResponse response = table.entry(WayangA2aJsonRpcMethods.GET_TASK)
                .orElseThrow()
                .dispatch(request, WayangA2aSendMessagePreflight.JsonRpcResult.empty());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("method", WayangA2aJsonRpcMethods.GET_TASK)
                .containsEntry("id", "dispatch-1");
    }

    private static Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlersForAllMethods() {
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers = new LinkedHashMap<>();
        for (String method : WayangA2aJsonRpcMethods.methods()) {
            handlers.put(method, okHandler(method));
        }
        return handlers;
    }

    private static WayangA2aJsonRpcMethodDispatchTable.Handler okHandler(String method) {
        return (request, preflight) -> new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(Map.of(
                        "method", method,
                        "id", request.id())),
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON));
    }
}
