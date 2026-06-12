package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcDiagnosticRouteGuardTest {

    @Test
    void acceptsGetRequestsThatCanReceiveJson() {
        Optional<WayangA2aHttpResponse> response = WayangA2aJsonRpcDiagnosticRouteGuard.validateGetJson(
                WayangA2aJsonRpcHttpRequests.getJson("/a2a/jsonrpc/diagnostics"),
                "/a2a/jsonrpc/diagnostics",
                WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                "diagnostics report");

        assertThat(response).isEmpty();
    }

    @Test
    void dispatchGetJsonDecoratesSuccessfulResponsesWithRouteHeaders() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcDiagnosticRouteGuard.dispatchGetJson(
                WayangA2aJsonRpcHttpRequests.getJson("/a2a/jsonrpc/diagnostics"),
                "/a2a/jsonrpc/diagnostics",
                WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                "diagnostics report",
                () -> WayangA2aJsonRpcHttpResponses.json(
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                        "{\"passed\":true}"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT);
        assertThat(WayangA2aHttpJson.read(response.body())).containsEntry("passed", true);
    }

    @Test
    void rejectsNonGetRequestsWithRouteHeaders() {
        WayangA2aHttpRequest request = new WayangA2aHttpRequest(
                "POST",
                "/a2a/jsonrpc/diagnostics",
                "{}",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of());

        WayangA2aHttpResponse response = WayangA2aJsonRpcDiagnosticRouteGuard.validateGetJson(
                        request,
                        "/a2a/jsonrpc/diagnostics",
                        WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                        "diagnostics report")
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(error(response))
                .containsEntry("code", "method_not_allowed")
                .containsEntry("message",
                        "A2A JSON-RPC diagnostics report path /a2a/jsonrpc/diagnostics requires GET.");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS);
    }

    @Test
    void rejectsRequestsThatDoNotAcceptJson() {
        WayangA2aHttpRequest request = new WayangA2aHttpRequest(
                "GET",
                "/a2a/jsonrpc/diagnostics",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, "text/plain"),
                Map.of());

        WayangA2aHttpResponse response = WayangA2aJsonRpcDiagnosticRouteGuard.validateGetJson(
                        request,
                        "/a2a/jsonrpc/diagnostics",
                        WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT,
                        WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                        "diagnostics report")
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(error(response))
                .containsEntry("code", "not_acceptable")
                .containsEntry("message",
                        "A2A JSON-RPC diagnostics report path /a2a/jsonrpc/diagnostics produces "
                                + WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON
                                + ", but Accept was text/plain.");
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Map<String, Object> body = WayangA2aHttpJson.read(response.body());
        return WayangA2aMaps.copyMap((Map<?, ?>) body.get("error"));
    }
}
