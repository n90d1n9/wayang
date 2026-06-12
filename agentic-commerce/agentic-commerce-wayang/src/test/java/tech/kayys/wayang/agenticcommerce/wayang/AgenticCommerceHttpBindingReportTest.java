package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceRouteCatalog;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceHttpBindingReportTest {

    @Test
    void reportsConfiguredCheckoutHttpBindingSurface() {
        AgenticCommerceHttpAdapterConfig config = AgenticCommerceHttpAdapterConfig.builder()
                .checkoutBasePath("/commerce/acp")
                .smokePath("/internal/acp/smoke")
                .bindingReportPath("/internal/acp/binding")
                .smokeEnabled(false)
                .build();
        AgenticCommerceHttpBindingReport report = AgenticCommerceHttpBindingReport.fromConfig(config);

        Map<String, Object> values = report.toMap();
        Map<String, Object> checkout = map(values.get("checkout"));
        Map<String, Object> smoke = map(values.get("smoke"));
        Map<String, Object> bindingReport = map(values.get("bindingReport"));
        List<Map<String, Object>> routes = maps(values.get("routes"));

        assertThat(values)
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsEntry("specVersion", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("routeCount", AgenticCommerceRouteCatalog.checkoutCatalog().routeCount());
        assertThat(checkout)
                .containsEntry("basePath", "/commerce/acp")
                .containsEntry("requestMediaType", AgenticCommerceProtocol.MIME_JSON)
                .containsEntry("responseMediaType", AgenticCommerceProtocol.MIME_JSON);
        assertThat(smoke)
                .containsEntry("enabled", false)
                .containsEntry("path", "/internal/acp/smoke")
                .containsEntry("allow", "GET, OPTIONS");
        assertThat(bindingReport)
                .containsEntry("enabled", true)
                .containsEntry("path", "/internal/acp/binding")
                .containsEntry("allow", "GET, OPTIONS");
        assertThat(routes)
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                        .containsEntry("httpMethod", "POST")
                        .containsEntry("publicPathTemplate", "/commerce/acp/checkout_sessions")
                        .containsEntry("requestBodyRequired", true))
                .anySatisfy(route -> assertThat(route)
                        .containsEntry("operation", AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION)
                        .containsEntry("httpMethod", "GET")
                        .containsEntry("requestBodyRequired", false));
        assertThat(map(values.get("config")))
                .containsEntry("checkoutBasePath", "/commerce/acp")
                .containsEntry("bindingReportPath", "/internal/acp/binding");
    }

    @Test
    void exposesReportThroughHttpResponseMetadata() {
        AgenticCommerceHttpBindingReport report = AgenticCommerceHttpBindingReport.defaults();

        var response = report.response();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(AgenticCommerceProtocol.MIME_JSON);
        assertThat(response.headers())
                .containsEntry(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ROUTE_OPERATION,
                        AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(AgenticCommerceJson.readObject(response.body()))
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsEntry("routeCount", (long) AgenticCommerceRouteCatalog.checkoutCatalog().routeCount());
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(entry -> {
                    assertThat(entry).isInstanceOf(Map.class);
                    return AgenticCommerceWayangMaps.copy((Map<?, ?>) entry);
                })
                .toList();
    }
}
