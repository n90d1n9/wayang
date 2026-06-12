package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRouteTableTest {

    @Test
    void findsEnabledRoutesByPath() {
        WayangA2aJsonRpcHttpRouteTable table = WayangA2aJsonRpcHttpRouteTable.fromConfig(
                WayangA2aJsonRpcHttpConfig.builder()
                        .endpointPath("/a2a/rpc")
                        .smokePath("/internal/a2a/smoke")
                        .build());

        assertThat(table.routeFor(WayangA2aHttpRequest.get("/a2a/rpc")))
                .get()
                .returns(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT,
                        WayangA2aJsonRpcHttpRouteDescriptor::key)
                .returns("/a2a/rpc", WayangA2aJsonRpcHttpRouteDescriptor::path);
        assertThat(table.routeFor(WayangA2aHttpRequest.get("/internal/a2a/smoke")))
                .get()
                .returns(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                        WayangA2aJsonRpcHttpRouteDescriptor::key);
    }

    @Test
    void omitsDisabledRoutesFromPathMatching() {
        WayangA2aJsonRpcHttpRouteTable table = WayangA2aJsonRpcHttpRouteTable.fromConfig(
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokePath("/internal/a2a/smoke")
                        .smokeEnabled(false)
                        .build());

        assertThat(table.routeFor(WayangA2aHttpRequest.get("/internal/a2a/smoke"))).isEmpty();
    }

    @Test
    void dispatchShortCircuitsOptionsWithoutCallingHandler() {
        WayangA2aJsonRpcHttpRouteTable table = WayangA2aJsonRpcHttpRouteTable.fromConfig(
                WayangA2aJsonRpcHttpConfig.defaults());
        AtomicBoolean called = new AtomicBoolean(false);

        WayangA2aHttpResponse response = table.dispatch(
                options("/"),
                (route, request) -> {
                    called.set(true);
                    return WayangA2aJsonRpcHttpResponses.json(route.operation(), "{}");
                });

        assertThat(called).isFalse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC);
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("operation", WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC)
                .containsEntry("path", "/");
    }

    @Test
    void dispatchInvokesHandlerForMatchedNonOptionsRoutes() {
        WayangA2aJsonRpcHttpRouteTable table = WayangA2aJsonRpcHttpRouteTable.fromConfig(
                WayangA2aJsonRpcHttpConfig.defaults());

        WayangA2aHttpResponse response = table.dispatch(
                WayangA2aHttpRequest.get(WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH),
                (route, request) -> WayangA2aJsonRpcHttpResponses.json(route.operation(), "{\"handled\":true}"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("handled", true);
    }

    @Test
    void dispatchReturnsJsonRpcPathNotFoundForUnknownOrDisabledPaths() {
        WayangA2aJsonRpcHttpRouteTable table = WayangA2aJsonRpcHttpRouteTable.fromConfig(
                WayangA2aJsonRpcHttpConfig.builder()
                        .smokePath("/internal/a2a/smoke")
                        .smokeEnabled(false)
                        .build());

        WayangA2aHttpResponse unknown = table.dispatch(
                WayangA2aHttpRequest.get("/missing"),
                (route, request) -> WayangA2aJsonRpcHttpResponses.json(route.operation(), "{}"));
        WayangA2aHttpResponse disabled = table.dispatch(
                WayangA2aHttpRequest.get("/internal/a2a/smoke"),
                (route, request) -> WayangA2aJsonRpcHttpResponses.json(route.operation(), "{}"));

        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(error(unknown))
                .containsEntry("code", "jsonrpc_path_not_found")
                .containsEntry("message", "No A2A JSON-RPC path matches /missing.");
        assertThat(disabled.statusCode()).isEqualTo(404);
        assertThat(error(disabled))
                .containsEntry("code", "jsonrpc_path_not_found")
                .containsEntry("message", "No A2A JSON-RPC path matches /internal/a2a/smoke.");
    }

    private static WayangA2aHttpRequest options(String path) {
        return new WayangA2aHttpRequest("OPTIONS", path, "", Map.of(), Map.of());
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
