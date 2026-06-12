package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiBridgeResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointBinding;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRouteBinding;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRouteCatalog;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRouteProjectionTest {

    @Test
    void projectsOrderedRouteEnvelope() {
        Map<String, Object> values = HttpRouteProjection.route(WayangA2uiHttpRoute.exchange());

        assertThat(values.keySet()).containsExactly(
                "operation",
                "method",
                "allowedMethods",
                "allowHeader",
                "path",
                "requestContentType",
                "responseContentType",
                "requestBodyRequired");
        assertThat(values)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("method", "POST")
                .containsEntry("allowHeader", "POST, OPTIONS")
                .containsEntry("path", WayangA2uiHttpRoute.PATH_EXCHANGE)
                .containsEntry("requestBodyRequired", true);
        assertThat((Iterable<String>) values.get("allowedMethods"))
                .containsExactly("POST", "OPTIONS");
    }

    @Test
    void projectsPublishedBindingEnvelope() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();
        WayangA2uiHttpRouteBinding binding = endpoint.binding("GET", "/api/a2ui/readiness")
                .orElseThrow();

        Map<String, Object> values = HttpRouteProjection.binding(binding);

        assertThat(values.keySet()).containsExactly(
                "operation",
                "method",
                "allowedMethods",
                "allowHeader",
                "path",
                "requestContentType",
                "responseContentType",
                "requestBodyRequired",
                "published");
        assertThat(values)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_READINESS)
                .containsEntry("path", "/api/a2ui/readiness")
                .containsEntry("published", true)
                .containsEntry("requestBodyRequired", false);
    }

    @Test
    void projectsCatalogEnvelopeAndRecordDelegates() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog()
                .mountedAt("/api/a2ui");

        Map<String, Object> values = HttpRouteProjection.catalog(catalog);

        assertThat(catalog.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly("routeCount", "routes");
        assertThat(values).containsEntry("routeCount", 6);
        assertThat((Iterable<Map<String, Object>>) values.get("routes"))
                .first()
                .satisfies(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                        .containsEntry("path", "/api/a2ui/exchange"));
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
