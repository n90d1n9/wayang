package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointPublicationTest {

    @Test
    void publishesMountedEndpointRouteManifest() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();
        WayangA2uiHttpEndpointPublication publication = endpoint.publication();

        assertThat(publication.routeCount()).isEqualTo(6);
        assertThat(publication.operations()).containsExactly(
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                WayangA2uiHttpRoute.OPERATION_SMOKE,
                WayangA2uiHttpRoute.OPERATION_READINESS);
        assertThat(publication.paths()).containsExactly(
                "/api/a2ui/exchange",
                "/api/a2ui/surface-catalog",
                "/api/a2ui/route-catalog",
                "/api/a2ui/binding-report",
                "/api/a2ui/smoke",
                "/api/a2ui/readiness");
        assertThat(publication.routes())
                .allSatisfy(route -> assertThat(route).containsEntry("published", true));
    }

    @Test
    void looksUpPublishedRoutesByOperationAndRawPath() {
        WayangA2uiHttpEndpointPublication publication = endpoint().publication();

        assertThat(publication.routeForOperation(WayangA2uiHttpRoute.OPERATION_EXCHANGE))
                .hasValueSatisfying(route -> assertThat(route)
                        .containsEntry("path", "/api/a2ui/exchange")
                        .containsEntry("method", "POST"));
        assertThat(publication.routeForPath("/api/a2ui/exchange?tenant=demo#ignored"))
                .hasValueSatisfying(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                        .containsEntry("allowHeader", "POST, OPTIONS"));
        assertThat(publication.routeForOperation("")).isEmpty();
        assertThat(publication.routeForPath("/a2ui/exchange")).isEmpty();
    }

    @Test
    void serializesPublicationSummaryAndRoutes() {
        WayangA2uiHttpEndpointPublication publication = endpoint().publication();

        Map<String, Object> values = publication.toMap();
        String publicationJson = TransportJson.json(
                values,
                "Unable to encode endpoint publication fixture");

        assertThat(values)
                .containsEntry("routeCount", 6)
                .containsEntry("operations", publication.operations())
                .containsEntry("paths", publication.paths());
        assertThat(publicationJson).startsWith("{\"routeCount\":");
        assertThat(publicationJson.indexOf("\"routes\""))
                .isGreaterThan(publicationJson.indexOf("\"paths\""));
        assertThat((Iterable<Map<String, Object>>) values.get("routes"))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                        .containsEntry("path", "/api/a2ui/exchange")
                        .containsEntry("published", true)
                        .containsEntry("requestBodyRequired", true)
                        .satisfies(entry -> assertThat((Iterable<String>) entry.get("allowedMethods"))
                                .containsExactly("POST", "OPTIONS")))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_READINESS)
                        .containsEntry("path", "/api/a2ui/readiness")
                        .containsEntry("published", true)
                        .containsEntry("requestBodyRequired", false)
                        .satisfies(entry -> assertThat((Iterable<String>) entry.get("allowedMethods"))
                                .containsExactly("GET", "OPTIONS")));
    }

    @Test
    void defensivelyCopiesPublishedRouteMaps() {
        WayangA2uiHttpEndpointPublication publication = new WayangA2uiHttpEndpointPublication(List.of(
                Map.of(
                        "operation",
                        "demo.operation",
                        "path",
                        "/demo",
                        "ignored",
                        Map.of("nested", "value"))));

        assertThat(publication.routeCount()).isEqualTo(1);
        assertThat(publication.operations()).containsExactly("demo.operation");
        assertThat(publication.paths()).containsExactly("/demo");
        assertThat(publication.routeForPath("demo"))
                .hasValueSatisfying(route -> assertThat(route).containsEntry("path", "/demo"));
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
