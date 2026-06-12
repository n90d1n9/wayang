package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticPlanTest {

    @Test
    void exposesCanonicalDefaultPlanFactoryAndConfigFallback() {
        WayangA2uiHttpEndpointDiagnosticPlan defaultPlan =
                WayangA2uiHttpEndpointDiagnosticPlan.defaultPlan();
        WayangA2uiHttpEndpointDiagnosticPlan nullBackedPlan =
                new WayangA2uiHttpEndpointDiagnosticPlan(null, null, null, null);

        assertThat(defaultPlan).isEqualTo(WayangA2uiHttpEndpointDiagnosticPlan.defaults());
        assertThat(defaultPlan.config()).isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
        assertThat(nullBackedPlan.config()).isEqualTo(WayangA2uiHttpEndpointDiagnosticConfig.defaultConfig());
    }

    @Test
    void decodesPlanMapsWithNestedConfigRequestsAndAttributes() {
        WayangA2uiHttpEndpointDiagnosticPlan plan =
                WayangA2uiHttpEndpointDiagnosticPlan.fromMap(Map.of(
                        "id",
                        "external-plan",
                        "config",
                        Map.of(
                                "profile",
                                "discovery",
                                "headers",
                                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT,
                                        List.of(WayangA2uiTransportContent.MIME_JSON)),
                                "attributes",
                                Map.of("tenant", "demo")),
                        "requests",
                        List.of(Map.of(
                                "method",
                                "GET",
                                "path",
                                "/api/a2ui/route-catalog?tenant=demo")),
                        "attributes",
                        Map.of("source", "cli")));

        assertThat(plan.diagnosticsId()).isEqualTo("external-plan");
        assertThat(plan.usesDefaultRequests()).isFalse();
        assertThat(plan.requestCount()).isEqualTo(1);
        assertThat(plan.config().smokeProbe()).isFalse();
        assertThat(plan.config().readinessProbe()).isFalse();
        assertThat(plan.config().defaultHeaders())
                .containsEntry(WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON));
        assertThat(plan.config().defaultAttributes()).containsEntry("tenant", "demo");
        assertThat(plan.requests().get(0).rawPath()).isEqualTo("/api/a2ui/route-catalog?tenant=demo");
        assertThat(plan.attributes()).containsEntry("source", "cli");
        assertThat(plan.toMap())
                .containsEntry("diagnosticsId", "external-plan")
                .containsEntry("requestCount", 1)
                .containsEntry("usesDefaultRequests", false);
    }

    @Test
    void roundTripsPlanJson() {
        WayangA2uiHttpEndpointDiagnosticPlan plan = new WayangA2uiHttpEndpointDiagnosticPlan(
                "json-plan",
                WayangA2uiHttpEndpointDiagnosticConfig.discoveryOnly(),
                List.of(WayangA2uiHttpEndpointDiagnosticRequest.get("/api/a2ui/route-catalog")),
                Map.of("source", "json"));

        WayangA2uiHttpEndpointDiagnosticPlan decoded =
                WayangA2uiHttpEndpointDiagnosticPlan.fromJson(plan.toJson());

        assertThat(decoded).isEqualTo(plan);
        assertThat(decoded.toJson())
                .contains("\"diagnosticsId\":\"json-plan\"")
                .contains("\"usesDefaultRequests\":false");
    }

    @Test
    void supportsDefaultRequestPlansFromTopLevelConfigKeys() {
        WayangA2uiHttpEndpointDiagnosticPlan plan =
                WayangA2uiHttpEndpointDiagnosticPlan.fromMap(Map.of(
                        "diagnosticsId",
                        "default-plan",
                        "profile",
                        "discovery",
                        "routeOptionsProbe",
                        "true",
                        "defaultAttributes",
                        Map.of("tenant", "demo"),
                        "attributes",
                        Map.of("source", "operator")));

        assertThat(plan.usesDefaultRequests()).isTrue();
        assertThat(plan.config().routeCatalogProbe()).isTrue();
        assertThat(plan.config().bindingReportProbe()).isTrue();
        assertThat(plan.config().routeOptionsProbe()).isTrue();
        assertThat(plan.config().smokeProbe()).isFalse();
        assertThat(plan.config().defaultAttributes()).containsEntry("tenant", "demo");
        assertThat(plan.attributes()).containsEntry("source", "operator");
    }

    @Test
    void rejectsBlankOrInvalidPlanJson() {
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticPlan.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic plan JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticPlan.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI HTTP endpoint diagnostic plan JSON");
    }
}
