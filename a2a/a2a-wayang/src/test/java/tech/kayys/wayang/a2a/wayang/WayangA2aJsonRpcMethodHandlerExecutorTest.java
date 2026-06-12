package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodHandlerExecutorTest {

    private final WayangA2aJsonRpcMethodHandlerExecutor executor =
            WayangA2aJsonRpcMethodHandlerExecutor.create();

    @Test
    void executesResolvedMethodHandler() {
        WayangA2aJsonRpcRequest request = request("ok-1", WayangA2aJsonRpcMethods.GET_TASK);
        WayangA2aJsonRpcMethodDispatchTable.Entry entry = entry(
                WayangA2aJsonRpcMethods.GET_TASK,
                (resolvedRequest, preflight) -> WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                        resolvedRequest.id(),
                        Map.of("ok", true)));

        WayangA2aHttpResponse response = executor.execute(request, entry, preflight());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(result(response)).containsEntry("ok", true);
    }

    @Test
    void mapsTaskLifecycleExceptionToUnsupportedOperation() {
        WayangA2aJsonRpcRequest request = request("lifecycle-1", WayangA2aJsonRpcMethods.CANCEL_TASK);
        WayangA2aJsonRpcMethodDispatchTable.Entry entry = entry(
                WayangA2aJsonRpcMethods.CANCEL_TASK,
                (resolvedRequest, preflight) -> {
                    throw new WayangA2aTaskLifecycleException("Task cannot be canceled.");
                });

        WayangA2aHttpResponse response = executor.execute(request, entry, preflight());

        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION)
                .containsEntry("message", "Task cannot be canceled.");
    }

    @Test
    void mapsIllegalArgumentExceptionToInvalidParams() {
        WayangA2aJsonRpcRequest request = request("params-1", WayangA2aJsonRpcMethods.GET_TASK);
        WayangA2aJsonRpcMethodDispatchTable.Entry entry = entry(
                WayangA2aJsonRpcMethods.GET_TASK,
                (resolvedRequest, preflight) -> {
                    throw new IllegalArgumentException("Task id is required.");
                });

        WayangA2aHttpResponse response = executor.execute(request, entry, preflight());

        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.INVALID_PARAMS)
                .containsEntry("message", "Task id is required.");
    }

    @Test
    void mapsRuntimeExceptionToInternalError() {
        WayangA2aJsonRpcRequest request = request("runtime-1", WayangA2aJsonRpcMethods.GET_TASK);
        WayangA2aJsonRpcMethodDispatchTable.Entry entry = entry(
                WayangA2aJsonRpcMethods.GET_TASK,
                (resolvedRequest, preflight) -> {
                    throw new IllegalStateException("Store is unavailable.");
                });

        WayangA2aHttpResponse response = executor.execute(request, entry, preflight());

        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.INTERNAL_ERROR)
                .containsEntry("message", "Store is unavailable.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("result");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("error");
    }

    private static WayangA2aJsonRpcMethodDispatchTable.Entry entry(
            String method,
            WayangA2aJsonRpcMethodDispatchTable.Handler handler) {
        return WayangA2aJsonRpcMethodDispatchTable.of(Map.of(method, handler))
                .entry(method)
                .orElseThrow();
    }

    private static WayangA2aJsonRpcRequest request(String id, String method) {
        return WayangA2aJsonRpcRequest.of(id, method, Map.of());
    }

    private static WayangA2aSendMessagePreflight.JsonRpcResult preflight() {
        return WayangA2aSendMessagePreflight.JsonRpcResult.empty();
    }
}
