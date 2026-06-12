package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRouteHandlersTest {

    @Test
    void dispatchesDiagnosticRouteThroughSuppliedReport() {
        WayangA2aHttpResponse response = handlers().dispatch(
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG),
                WayangA2aJsonRpcHttpRequests.getJson(WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG);
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("routeCount", 8);
    }

    @Test
    void reportsUnconfiguredSmokeRunnerFromSmokeRoute() {
        WayangA2aHttpResponse response = handlers().dispatch(
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE),
                WayangA2aJsonRpcHttpRequests.getJson(WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH));

        assertThat(response.statusCode()).isEqualTo(501);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_SMOKE);
        assertThat(error(response))
                .containsEntry("code", "jsonrpc_smoke_not_configured")
                .containsEntry("message", "A2A JSON-RPC smoke runner is not configured.");
    }

    @Test
    void delegatesEndpointValidationBeforeJsonRpcDispatch() {
        WayangA2aHttpResponse response = handlers().dispatch(
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT),
                WayangA2aHttpRequest.get(WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH));

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT);
        assertThat(error(response))
                .containsEntry("code", "method_not_allowed")
                .containsEntry("message", "A2A JSON-RPC endpoint / requires POST.");
    }

    private static WayangA2aJsonRpcHttpRouteHandlers handlers() {
        WayangA2aJsonRpcDispatcher dispatcher = WayangA2aJsonRpcDispatcher.forExecution(
                A2aAgentCard.minimal(
                        "Wayang",
                        "A2A JSON-RPC endpoint",
                        "https://wayang.test/a2a",
                        List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat")))),
                new InMemoryWayangA2aTaskStore(),
                request -> AgentResponse.builder()
                        .runId("run-route-handlers")
                        .requestId(request.requestId())
                        .answer("pong")
                        .build());
        return new WayangA2aJsonRpcHttpRouteHandlers(
                dispatcher,
                WayangA2aExtensionNegotiator.fromAgentCard(dispatcher.agentCard()),
                WayangA2aExtendedAgentCardAuthorizer.allowAll(),
                diagnosticHandlers());
    }

    private static WayangA2aJsonRpcHttpDiagnosticHandlers diagnosticHandlers() {
        return WayangA2aJsonRpcHttpDiagnosticHandlers.from(
                null,
                unused(),
                () -> WayangA2aJsonRpcHttpRouteCatalog.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()),
                unused(),
                unused(),
                unused(),
                unused());
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor route(String key) {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults())
                .stream()
                .filter(route -> route.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static <T> Supplier<T> unused() {
        return () -> {
            throw new AssertionError("Unexpected report supplier invocation");
        };
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
