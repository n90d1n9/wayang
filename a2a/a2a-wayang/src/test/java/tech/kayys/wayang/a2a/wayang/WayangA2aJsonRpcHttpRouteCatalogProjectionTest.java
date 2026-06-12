package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRouteCatalogProjectionTest {

    @Test
    void keepsOrderedCatalogEnvelope() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokeEnabled(false)
                .routeCatalogPath("/internal/a2a/routes")
                .build();
        WayangA2aJsonRpcHttpRouteCatalog catalog = WayangA2aJsonRpcHttpRouteCatalog.fromConfig(config);

        Map<String, Object> values = WayangA2aJsonRpcHttpRouteCatalogProjection.catalog(catalog);

        assertThat(values.keySet()).containsExactly(
                "binding",
                "protocolVersion",
                "routeCount",
                "enabledRouteCount",
                "routes");
        assertThat(values)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("routeCount", 8)
                .containsEntry("enabledRouteCount", 7);
        assertThat(WayangA2aMaps.objectList(values.get("routes")))
                .hasSize(8)
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation",
                                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                        .containsEntry("path", "/internal/a2a/routes")
                        .containsEntry("httpMethod", "GET"));
    }

    @Test
    void buildsRouteCatalogResponseThroughProjection() {
        WayangA2aJsonRpcHttpRouteCatalog catalog =
                WayangA2aJsonRpcHttpRouteCatalog.fromConfig(WayangA2aJsonRpcHttpConfig.defaults());

        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpRouteCatalogProjection.response(catalog);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION);
        assertThat(response.body()).isEqualTo(catalog.toJson());
        assertThat(catalog.response().body()).isEqualTo(response.body());
    }

    @Test
    void recordDelegatesToProjectionForCatalogMap() {
        WayangA2aJsonRpcHttpRouteCatalog catalog =
                WayangA2aJsonRpcHttpRouteCatalog.fromConfig(WayangA2aJsonRpcHttpConfig.defaults());

        assertThat(catalog.toMap()).isEqualTo(WayangA2aJsonRpcHttpRouteCatalogProjection.catalog(catalog));
    }
}
