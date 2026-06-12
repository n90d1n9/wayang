package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpRouteCatalogTest {

    @Test
    void keepsCanonicalCatalogForBlankMountRoot() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog();

        assertThat(catalog.mountedAt("")).isSameAs(catalog);
        assertThat(catalog.route("GET", WayangA2uiHttpRoute.PATH_ROUTE_CATALOG))
                .contains(WayangA2uiHttpRoute.routeCatalog());
        assertThat(catalog.specAlignmentReport().aligned()).isTrue();
    }

    @Test
    void mountsDefaultRoutesAtCustomRoot() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog()
                .mountedAt(" /api/a2ui/ ");

        assertThat(catalog.routeCount()).isEqualTo(6);
        assertThat(catalog.route("GET", "/api/a2ui/route-catalog"))
                .contains(WayangA2uiHttpRoute.routeCatalog().withPath("/api/a2ui/route-catalog"));
        assertThat(catalog.route("POST", "/api/a2ui/exchange"))
                .contains(WayangA2uiHttpRoute.exchange().withPath("/api/a2ui/exchange"));
        assertThat(catalog.route("GET", WayangA2uiHttpRoute.PATH_ROUTE_CATALOG)).isEmpty();
    }

    @Test
    void supportsRootMountedRoutes() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog()
                .mountedAt("/");

        assertThat(catalog.route("GET", "/route-catalog"))
                .contains(WayangA2uiHttpRoute.routeCatalog().withPath("/route-catalog"));
        assertThat(catalog.route("GET", WayangA2uiHttpRoute.PATH_ROUTE_CATALOG)).isEmpty();
    }

    @Test
    void bridgeAdapterHandlesMountedCatalogRoutes() {
        WayangA2uiHttpRouteCatalog catalog = WayangA2uiHttpRouteCatalog.defaultCatalog()
                .mountedAt("/api/a2ui");
        WayangA2uiHttpBridgeAdapter adapter = new WayangA2uiHttpBridgeAdapter(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                catalog,
                WayangA2uiHttpRouteGuard.strict());

        WayangA2uiHttpResponse response = adapter.handle(WayangA2uiHttpRequest.get("/api/a2ui/route-catalog"));
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(transport.body()).contains("\"path\":\"/api/a2ui/exchange\"");
        assertThat(adapter.handle(WayangA2uiHttpRequest.routeCatalog()).statusCode()).isEqualTo(404);
    }
}
