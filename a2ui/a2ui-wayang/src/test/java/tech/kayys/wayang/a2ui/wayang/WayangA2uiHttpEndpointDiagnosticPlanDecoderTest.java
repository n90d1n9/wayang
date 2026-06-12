package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticPlanDecoderTest {

    @Test
    void decodesNullAndEmptyMapsAsCanonicalDefaultPlan() {
        assertThat(WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromMap(null))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticPlan.defaultPlan());
        assertThat(WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromMap(Map.of()))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticPlan.defaultPlan());
    }

    @Test
    void decodesFlattenedExternalPlanAliases() {
        WayangA2uiHttpEndpointDiagnosticPlan plan =
                WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromMap(Map.of(
                        "id",
                        "decoder-plan",
                        "profile",
                        "discovery",
                        "routeOptionsProbe",
                        "true",
                        "headers",
                        Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                List.of(WayangA2uiTransportContent.MIME_JSON)),
                        "requests",
                        List.of(Map.of(
                                "method",
                                "post",
                                "path",
                                "api/a2ui/exchange",
                                "body",
                                "{}")),
                        "attributes",
                        Map.of("source", "decoder")));

        assertThat(plan.diagnosticsId()).isEqualTo("decoder-plan");
        assertThat(plan.config().smokeProbe()).isFalse();
        assertThat(plan.config().routeOptionsProbe()).isTrue();
        assertThat(plan.config().defaultHeaders())
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON));
        assertThat(plan.requests())
                .singleElement()
                .satisfies(request -> assertThat(request)
                        .returns("POST", WayangA2uiHttpEndpointDiagnosticRequest::method)
                        .returns("/api/a2ui/exchange", WayangA2uiHttpEndpointDiagnosticRequest::rawPath)
                        .returns("{}", WayangA2uiHttpEndpointDiagnosticRequest::body));
        assertThat(plan.attributes()).containsEntry("source", "decoder");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "diagnosticsId",
                "delegated-plan",
                "config",
                Map.of("profile", "discovery-only"),
                "requests",
                List.of(Map.of("method", "GET", "rawPath", "/api/a2ui/route-catalog")));

        assertThat(WayangA2uiHttpEndpointDiagnosticPlan.fromMap(values))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromMap(values));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpEndpointDiagnosticPlan decoded =
                WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromJson("""
                        {
                          "diagnosticsId": "json-decoder-plan",
                          "config": {"profile": "discovery-only"},
                          "requests": [{"method": "GET", "path": "/api/a2ui/route-catalog"}]
                        }
                        """);

        assertThat(decoded.diagnosticsId()).isEqualTo("json-decoder-plan");
        assertThat(decoded.requestCount()).isEqualTo(1);
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticPlanDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic plan JSON must not be blank");
    }
}
