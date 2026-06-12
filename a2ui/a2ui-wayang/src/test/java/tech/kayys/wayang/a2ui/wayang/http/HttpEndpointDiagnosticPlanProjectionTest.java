package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticConfig;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticPlan;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpEndpointDiagnosticPlanProjectionTest {

    @Test
    void projectsOrderedConfigEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticConfig config =
                WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly()
                        .withDefaultHeaders(Map.of(
                                WayangA2uiHttpResponse.HEADER_ACCEPT,
                                List.of(WayangA2uiTransportContent.MIME_JSON)))
                        .withDefaultAttributes(Map.of("tenant", "demo"));

        Map<String, Object> values = HttpEndpointDiagnosticPlanProjection.config(config);

        assertThat(config.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_CATALOG_PROBE,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_BINDING_REPORT_PROBE,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_ROUTE_OPTIONS_PROBE,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_HEADERS,
                WayangA2uiHttpEndpointDiagnosticConfig.KEY_DEFAULT_ATTRIBUTES);
        assertThat(values)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticConfig.KEY_SMOKE_PROBE, false)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticConfig.KEY_READINESS_PROBE, false);
    }

    @Test
    void projectsOrderedRequestEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticRequest request =
                WayangA2uiHttpEndpointDiagnosticRequest.postJson(
                                "/api/a2ui/exchange",
                                "{\"kind\":\"json\"}")
                        .withHeaders(Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                WayangA2uiTransportContent.MIME_JSON))
                        .withAttributes(Map.of("tenant", "demo"));

        Map<String, Object> values = HttpEndpointDiagnosticPlanProjection.request(request);

        assertThat(request.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD,
                WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH,
                WayangA2uiHttpEndpointDiagnosticRequest.KEY_BODY,
                "bodyPresent",
                "bodyLength",
                WayangA2uiHttpEndpointDiagnosticRequest.KEY_HEADERS,
                WayangA2uiHttpEndpointDiagnosticRequest.KEY_ATTRIBUTES);
        assertThat(values)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD, "POST")
                .containsEntry(WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH, "/api/a2ui/exchange")
                .containsEntry("bodyPresent", true)
                .containsEntry("bodyLength", 15);
    }

    @Test
    void projectsOrderedPlanEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointDiagnosticPlan plan = new WayangA2uiHttpEndpointDiagnosticPlan(
                "projection-plan",
                WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly(),
                List.of(WayangA2uiHttpEndpointDiagnosticRequest.get("/api/a2ui/route-catalog")),
                Map.of("source", "projection"));

        Map<String, Object> values = HttpEndpointDiagnosticPlanProjection.plan(plan);

        assertThat(plan.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                WayangA2uiHttpEndpointDiagnosticPlan.KEY_DIAGNOSTICS_ID,
                WayangA2uiHttpEndpointDiagnosticPlan.KEY_CONFIG,
                WayangA2uiHttpEndpointDiagnosticPlan.KEY_REQUESTS,
                "requestCount",
                "usesDefaultRequests",
                WayangA2uiHttpEndpointDiagnosticPlan.KEY_ATTRIBUTES);
        assertThat(values)
                .containsEntry(WayangA2uiHttpEndpointDiagnosticPlan.KEY_DIAGNOSTICS_ID, "projection-plan")
                .containsEntry("requestCount", 1)
                .containsEntry("usesDefaultRequests", false);
        assertThat((Iterable<Map<String, Object>>) values.get(WayangA2uiHttpEndpointDiagnosticPlan.KEY_REQUESTS))
                .singleElement()
                .satisfies(request -> assertThat(request)
                        .containsEntry(WayangA2uiHttpEndpointDiagnosticRequest.KEY_METHOD, "GET")
                        .containsEntry(WayangA2uiHttpEndpointDiagnosticRequest.KEY_RAW_PATH,
                                "/api/a2ui/route-catalog"));
    }
}
