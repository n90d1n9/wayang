package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutItem;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeProbeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceHttpAdapterTest {

    @Test
    void dispatchesPrefixedCheckoutRoutesToConnector() {
        AgenticCommerceHttpAdapter adapter = AgenticCommerceHttpAdapter.of(new InMemoryAgenticCommerceConnector());
        AgenticCommerceHttpResponse response = adapter.dispatch(post(
                "/agentic-commerce/checkout_sessions",
                AgenticCommerceJson.write(new AgenticCommerceCreateCheckoutSessionRequest(
                        null,
                        List.of(new AgenticCommerceCheckoutItem("sku_1", "Wayang Tee", "", 1, 3000, Map.of())),
                        "usd",
                        null,
                        Map.of(),
                        List.of(),
                        Map.of(),
                        List.of(),
                        Map.of(),
                        "",
                        "",
                        "",
                        Map.of()).toMap())));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers())
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ROUTE_OPERATION,
                        AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "POST, OPTIONS")
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_SPEC_VERSION,
                        AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(AgenticCommerceJson.readObject(response.body()))
                .containsEntry("id", "cs_1")
                .containsEntry("status", "open")
                .containsEntry("currency", "USD");
    }

    @Test
    void handlesOptionsAndValidationErrors() {
        AgenticCommerceHttpAdapter adapter = AgenticCommerceHttpAdapter.of(new InMemoryAgenticCommerceConnector());

        AgenticCommerceHttpResponse options = adapter.dispatch(new AgenticCommerceHttpRequest(
                "OPTIONS",
                "/agentic-commerce/checkout_sessions/cs_1",
                "",
                Map.of(),
                Map.of()));
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers())
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "GET, POST, OPTIONS");
        assertThat(AgenticCommerceJson.readObject(options.body()))
                .containsEntry("operation", AgenticCommerceHttpAdapter.OPERATION_CHECKOUT_OPTIONS)
                .containsEntry("checkoutBasePath", "/agentic-commerce");

        assertThat(error(adapter.dispatch(new AgenticCommerceHttpRequest(
                "GET",
                "/agentic-commerce/checkout_sessions",
                "",
                validHeaders(false),
                Map.of()))))
                .containsEntry("code", "method_not_allowed");
        assertThat(error(adapter.dispatch(new AgenticCommerceHttpRequest(
                "POST",
                "/agentic-commerce/checkout_sessions",
                "{}",
                Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON),
                Map.of()))))
                .containsEntry("code", "missing_authorization");
        assertThat(error(adapter.dispatch(new AgenticCommerceHttpRequest(
                "POST",
                "/agentic-commerce/checkout_sessions",
                "{}",
                validHeaders(false),
                Map.of()))))
                .containsEntry("code", "not_acceptable");
        assertThat(error(adapter.dispatch(AgenticCommerceHttpRequest.get("/missing"))))
                .containsEntry("code", "checkout_path_not_found");
    }

    @Test
    void servesSmokeEndpointAndHonorsConfiguredPaths() {
        AgenticCommerceHttpAdapterConfig config = AgenticCommerceHttpAdapterConfig.fromMap(Map.of(
                "basePath",
                "/commerce/acp",
                "smokePath",
                "/internal/acp/smoke"));
        AgenticCommerceHttpAdapter adapter = AgenticCommerceHttpAdapter.configured(
                new InMemoryAgenticCommerceConnector(),
                config);

        AgenticCommerceHttpResponse create = adapter.dispatch(post(
                "/commerce/acp/checkout_sessions",
                AgenticCommerceJson.write(Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1000)),
                        "currency",
                        "usd"))));
        AgenticCommerceHttpResponse smoke = adapter.dispatch(new AgenticCommerceHttpRequest(
                "GET",
                "/internal/acp/smoke",
                "",
                Map.of(AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON),
                Map.of()));

        assertThat(adapter.config().toMap()).containsEntry("checkoutBasePath", "/commerce/acp");
        assertThat(create.statusCode()).isEqualTo(201);
        assertThat(smoke.statusCode()).isEqualTo(200);
        assertThat(smoke.headers())
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ROUTE_OPERATION,
                        AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_SMOKE_PASSED, true)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(AgenticCommerceJson.readObject(smoke.body()))
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0L);

        AgenticCommerceCheckoutHttpSmokeProbeResult probe = adapter.smokeProbe();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.summary().scenarioId()).isEqualTo("agentic-commerce-checkout-smoke");
    }

    @Test
    void servesBindingReportThroughHttpAdapter() {
        AgenticCommerceHttpAdapter adapter = AgenticCommerceHttpAdapter.of(new InMemoryAgenticCommerceConnector());

        AgenticCommerceHttpResponse response = adapter.dispatch(new AgenticCommerceHttpRequest(
                "GET",
                AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH,
                "",
                Map.of(AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON),
                Map.of()));
        AgenticCommerceHttpResponse options = adapter.dispatch(new AgenticCommerceHttpRequest(
                "OPTIONS",
                AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH,
                "",
                Map.of(),
                Map.of()));
        Map<String, Object> body = AgenticCommerceJson.readObject(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ROUTE_OPERATION,
                        AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT)
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "GET, OPTIONS")
                .containsEntry(AgenticCommerceHttpAdapter.HEADER_SPEC_VERSION,
                        AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(body)
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsEntry("routeCount", 5L);
        assertThat(map(body.get("bindingReport")))
                .containsEntry("path", AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH)
                .containsEntry("enabled", true);
        assertThat(options.statusCode()).isEqualTo(200);
        assertThat(options.headers()).containsEntry(AgenticCommerceHttpAdapter.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(AgenticCommerceJson.readObject(options.body()))
                .containsEntry("operation", AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT)
                .containsEntry("path", AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH);
    }

    @Test
    void disabledSmokePathFallsThroughToNotFound() {
        AgenticCommerceHttpAdapter adapter = AgenticCommerceHttpAdapter.configured(
                new InMemoryAgenticCommerceConnector(),
                AgenticCommerceHttpAdapterConfig.builder().smokeEnabled(false).build());

        assertThat(error(adapter.dispatch(AgenticCommerceHttpRequest.get("/agentic-commerce/smoke"))))
                .containsEntry("code", "checkout_path_not_found");
    }

    private static AgenticCommerceHttpRequest post(String path, String body) {
        return new AgenticCommerceHttpRequest(
                "POST",
                path,
                body,
                validHeaders(true),
                Map.of());
    }

    private static Map<String, Object> validHeaders(boolean jsonAccept) {
        Map<String, Object> headers = new java.util.LinkedHashMap<>();
        headers.put(AgenticCommerceProtocol.HEADER_AUTHORIZATION, "Bearer token");
        headers.put(AgenticCommerceProtocol.HEADER_API_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        headers.put(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        headers.put(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-test");
        headers.put(AgenticCommerceProtocol.HEADER_ACCEPT, jsonAccept ? AgenticCommerceProtocol.MIME_JSON : "text/plain");
        return Map.copyOf(headers);
    }

    private static Map<String, Object> error(AgenticCommerceHttpResponse response) {
        Object error = AgenticCommerceJson.readObject(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) error);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
