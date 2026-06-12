package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRouteProjectionTest {

    @Test
    void keepsOrderedRouteCatalogEntry() {
        WayangA2aJsonRpcHttpRoute route = routeDescriptor(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG).toRoute();

        Map<String, Object> values = WayangA2aJsonRpcHttpRouteProjection.route(route);

        assertThat(values.keySet()).containsExactly(
                "operation",
                "enabled",
                "path",
                "httpMethod",
                "allowedMethods",
                "allow",
                "requestMediaType",
                "responseMediaTypes",
                "requestBodyRequired");
        assertThat(values)
                .containsEntry("operation", WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("httpMethod", "GET")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG)
                .containsEntry("requestBodyRequired", false);
        assertThat(WayangA2aMaps.stringList(values.get("allowedMethods")))
                .containsExactly("GET", "OPTIONS");
        assertThat(WayangA2aMaps.stringList(values.get("responseMediaTypes")))
                .containsExactly(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(route.toMap()).isEqualTo(values);
    }

    @Test
    void keepsOrderedBindingReportEndpointEntry() {
        WayangA2aJsonRpcHttpRouteDescriptor endpoint =
                routeDescriptor(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT);

        Map<String, Object> values = WayangA2aJsonRpcHttpRouteProjection.bindingReportRoute(endpoint);

        assertThat(values.keySet()).containsExactly(
                "path",
                "httpMethod",
                "allow",
                "requestMediaType",
                "responseMediaTypes");
        assertThat(values)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH)
                .containsEntry("httpMethod", "POST")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT)
                .containsEntry("requestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .doesNotContainKey("enabled");
        assertThat(WayangA2aMaps.stringList(values.get("responseMediaTypes")))
                .containsExactly(
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(endpoint.toBindingReportMap()).isEqualTo(values);
    }

    @Test
    void keepsOrderedBindingReportDiagnosticEntry() {
        WayangA2aJsonRpcHttpRouteDescriptor route =
                routeDescriptor(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG);

        Map<String, Object> values = WayangA2aJsonRpcHttpRouteProjection.bindingReportRoute(route);

        assertThat(values.keySet()).containsExactly(
                "enabled",
                "path",
                "httpMethod",
                "allow",
                "responseMediaType");
        assertThat(values)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("httpMethod", "GET")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG)
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .doesNotContainKey("requestMediaType");
        assertThat(route.toBindingReportMap()).isEqualTo(values);
    }

    @Test
    void keepsOrderedOptionsPayloadAndResponse() {
        WayangA2aJsonRpcHttpRouteDescriptor route =
                routeDescriptor(WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG);

        Map<String, Object> payload = WayangA2aJsonRpcHttpRouteProjection.optionsPayload(route);
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpRouteProjection.optionsResponse(route);

        assertThat(payload.keySet()).containsExactly(
                "binding",
                "operation",
                "path",
                "allow",
                "protocolVersion");
        assertThat(payload)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("operation", WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG)
                .containsEntry("protocolVersion", A2aProtocol.VERSION);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG);
        assertThat(WayangA2aHttpJson.read(response.body())).isEqualTo(payload);
        assertThat(route.optionsResponse().body()).isEqualTo(response.body());
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor routeDescriptor(String key) {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults())
                .stream()
                .filter(route -> route.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
