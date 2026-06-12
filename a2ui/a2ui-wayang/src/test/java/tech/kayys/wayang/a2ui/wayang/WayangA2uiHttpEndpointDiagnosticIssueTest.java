package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticIssueTest {

    @Test
    void projectsUnknownEndpointPathIssues() {
        WayangA2uiHttpEndpointExchange exchange = diagnosticEndpoint().exchange(
                "GET",
                "/api/a2ui/missing?tenant=demo",
                "",
                Map.of(),
                Map.of("traceId", "trace-1"));

        WayangA2uiHttpEndpointDiagnosticIssue issue =
                WayangA2uiHttpEndpointDiagnosticIssue.from("diagnostics-1", 0, exchange).orElseThrow();

        assertThat(issue.exchangeIndex()).isEqualTo(1);
        assertThat(issue.knownPath()).isFalse();
        assertThat(issue.matched()).isFalse();
        assertThat(issue.statusCode()).isEqualTo(404);
        assertThat(issue.category())
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH);
        assertThat(issue.errorCode()).isEqualTo("not_found");
        assertThat(issue.message()).isEqualTo("Unknown A2UI HTTP route: GET /api/a2ui/missing");
        assertThat(issue.attributes()).containsEntry("traceId", "trace-1");
        assertThat(issue.toMap())
                .containsEntry("diagnosticsId", "diagnostics-1")
                .containsEntry("path", "/api/a2ui/missing")
                .containsEntry("allow", "");
        assertThat(issue.toJson())
                .contains("\"category\":\"unknown-path\"")
                .contains("\"errorCode\":\"not_found\"");
    }

    @Test
    void projectsKnownPathRouteMismatches() {
        WayangA2uiHttpEndpointExchange exchange = diagnosticEndpoint().exchange(
                "GET",
                "/api/a2ui/exchange",
                "",
                Map.of(),
                Map.of());

        WayangA2uiHttpEndpointDiagnosticIssue issue =
                WayangA2uiHttpEndpointDiagnosticIssue.from("diagnostics-1", 2, exchange).orElseThrow();

        assertThat(issue.exchangeIndex()).isEqualTo(2);
        assertThat(issue.knownPath()).isTrue();
        assertThat(issue.matched()).isFalse();
        assertThat(issue.category())
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH);
        assertThat(issue.errorCode()).isEqualTo("method_not_allowed");
        assertThat(issue.routeOperation()).isEqualTo(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(issue.allow()).contains("POST");
    }

    @Test
    void skipsSuccessfulMatchedExchanges() {
        WayangA2uiHttpEndpointExchange exchange = diagnosticEndpoint().exchange(
                "GET",
                "/api/a2ui/route-catalog",
                "",
                Map.of(),
                Map.of());

        assertThat(WayangA2uiHttpEndpointDiagnosticIssue.from("diagnostics-1", 1, exchange)).isEmpty();
    }

    private static WayangA2uiHttpEndpointBinding diagnosticEndpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
