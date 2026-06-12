package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcHttpDiagnosticHandlersTest {

    @Test
    void resolvesDiagnosticSupplierByRouteKey() {
        WayangA2aHttpResponse response = handlers()
                .responseSupplier(route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG))
                .get();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION);
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("enabledRouteCount", 8);
    }

    @Test
    void preservesRouteMetadataForUnconfiguredSmokeRunner() {
        WayangA2aHttpResponse response = handlers()
                .responseSupplier(route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE))
                .get();

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
    void rejectsEndpointRouteAsDiagnostic() {
        assertThatThrownBy(() -> handlers().responseSupplier(route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported A2A JSON-RPC diagnostic route key: endpoint");
    }

    @Test
    void reportsCompleteCoverageForDefaultDiagnosticRoutes() {
        WayangA2aJsonRpcHttpDiagnosticHandlers handlers = handlers();
        List<WayangA2aJsonRpcHttpRouteDescriptor> routes = WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(
                WayangA2aJsonRpcHttpConfig.defaults());

        assertThat(handlers.handlerKeys()).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
        assertThat(handlers.missingHandlerKeys(routes)).isEmpty();
        assertThat(handlers.orphanHandlerKeys(routes)).isEmpty();
        assertThat(handlers.completeFor(routes)).isTrue();
    }

    @Test
    void reportsMissingAndOrphanDiagnosticHandlers() {
        WayangA2aJsonRpcHttpDiagnosticHandlers handlers = new WayangA2aJsonRpcHttpDiagnosticHandlers(
                Map.of("orphan", route -> WayangA2aJsonRpcHttpResponses.json(route.operation(), "{}")));
        List<WayangA2aJsonRpcHttpRouteDescriptor> routes = List.of(
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT),
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE),
                route(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG));

        assertThat(handlers.handlerKeys()).containsExactly("orphan");
        assertThat(handlers.missingHandlerKeys(routes)).containsExactly(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG);
        assertThat(handlers.orphanHandlerKeys(routes)).containsExactly("orphan");
        assertThat(handlers.completeFor(routes)).isFalse();
        assertThatThrownBy(() -> handlers.requireCompleteFor(routes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing=[smoke, routeCatalog]")
                .hasMessageContaining("orphan=[orphan]");
    }

    private static WayangA2aJsonRpcHttpDiagnosticHandlers handlers() {
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
