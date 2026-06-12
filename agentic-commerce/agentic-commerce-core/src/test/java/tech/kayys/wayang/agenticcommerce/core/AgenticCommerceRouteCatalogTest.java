package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceRouteCatalogTest {

    @Test
    void exposesCheckoutRouteManifest() {
        AgenticCommerceRouteCatalog catalog = AgenticCommerceRouteCatalog.checkoutCatalog();

        assertThat(catalog.routeCount()).isEqualTo(5);
        assertThat(catalog.routes()).containsExactly(
                AgenticCommerceHttpRoute.createCheckoutSession(),
                AgenticCommerceHttpRoute.retrieveCheckoutSession(),
                AgenticCommerceHttpRoute.updateCheckoutSession(),
                AgenticCommerceHttpRoute.completeCheckoutSession(),
                AgenticCommerceHttpRoute.cancelCheckoutSession());
        assertThat(catalog.toMap())
                .containsEntry("protocol", "agentic-commerce")
                .containsEntry("specVersion", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("routeCount", 5);
        assertThat(catalog.specAlignmentReport().aligned()).isTrue();
    }

    @Test
    void matchesTemplatedCheckoutSessionRoutes() {
        AgenticCommerceRouteCatalog catalog = AgenticCommerceRouteCatalog.checkoutCatalog();

        assertThat(catalog.route(new AgenticCommerceHttpRequest(
                        "GET",
                        "/checkout_sessions/cs_123",
                        "",
                        commonHeaders(),
                        Map.of())))
                .contains(AgenticCommerceHttpRoute.retrieveCheckoutSession());
        assertThat(catalog.route(new AgenticCommerceHttpRequest(
                        "POST",
                        "/checkout_sessions/cs_123",
                        "{}",
                        commonHeaders(),
                        Map.of())))
                .contains(AgenticCommerceHttpRoute.updateCheckoutSession());
        assertThat(catalog.route(new AgenticCommerceHttpRequest(
                        "POST",
                        "/checkout_sessions/cs_123/complete",
                        "{}",
                        commonHeaders(),
                        Map.of())))
                .contains(AgenticCommerceHttpRoute.completeCheckoutSession());
        assertThat(catalog.route(new AgenticCommerceHttpRequest(
                        "POST",
                        "/checkout_sessions/cs_123/cancel",
                        "{}",
                        commonHeaders(),
                        Map.of())))
                .contains(AgenticCommerceHttpRoute.cancelCheckoutSession());
        assertThat(catalog.routeForOperation(AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION))
                .contains(AgenticCommerceHttpRoute.updateCheckoutSession());
    }

    private static Map<String, Object> commonHeaders() {
        return Map.of(
                AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                "Bearer token",
                AgenticCommerceProtocol.HEADER_API_VERSION,
                AgenticCommerceProtocol.SPEC_VERSION,
                AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                AgenticCommerceProtocol.MIME_JSON);
    }
}
