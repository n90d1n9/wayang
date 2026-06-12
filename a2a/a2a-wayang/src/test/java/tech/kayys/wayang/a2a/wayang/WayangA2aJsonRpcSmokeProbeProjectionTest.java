package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSmokeProbeProjectionTest {

    @Test
    void keepsOrderedSmokeHeaders() {
        WayangA2aJsonRpcSmokeResult result = smokeResult();

        Map<String, Object> headers = WayangA2aJsonRpcSmokeProbeProjection.smokeHeaders(result);

        assertThat(headers.keySet()).containsExactly(
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO);
        assertThat(headers)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED, false)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE,
                        WayangA2aJsonRpcSmokeResult.EXIT_FAILURE)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO,
                        "a2a.jsonrpc.probe");
    }

    @Test
    void combinesProtocolAndSmokeHeadersInProbeResponseOrder() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcSmokeProbeProjection.response(smokeResult());

        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE)
                .containsEntry(WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO,
                        "a2a.jsonrpc.probe");
    }

    @Test
    void keepsOrderedProbeEnvelope() {
        WayangA2aJsonRpcSmokeResult result = smokeResult();
        Map<String, Object> headers = WayangA2aJsonRpcSmokeProbeProjection.smokeHeaders(result);
        WayangA2aJsonRpcSmokeSummary summary = WayangA2aJsonRpcSmokeSummary.from(result);

        Map<String, Object> values = WayangA2aJsonRpcSmokeProbeProjection.probe(
                200,
                true,
                WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
                A2aProtocol.VERSION,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                true,
                false,
                WayangA2aJsonRpcSmokeResult.EXIT_FAILURE,
                summary,
                headers);

        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "protocolVersion",
                "contentType",
                "smokeRoute",
                "passed",
                "exitCode",
                "summary",
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 200)
                .containsEntry("routeOperation", WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE)
                .containsEntry("exitCode", WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
        assertThat(map(values.get("summary")).keySet()).containsExactly(
                "passed",
                "exitCode",
                "scenarioId",
                "exchangeCount",
                "issueCount",
                "smokeResult",
                "successfulExit",
                "issues",
                "attributes",
                "body");
        assertThat(map(values.get("headers")).keySet()).containsExactly(
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_PASSED,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_EXIT_CODE,
                WayangA2aJsonRpcSmokeProbeResult.HEADER_A2A_SMOKE_SCENARIO);
    }

    private static WayangA2aJsonRpcSmokeResult smokeResult() {
        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.methodError(
                "a2a.jsonrpc.probe",
                "UnknownMethod");
        WayangA2aJsonRpcScenarioResult scenarioResult = new WayangA2aJsonRpcScenarioResult(
                scenario,
                List.of(),
                List.of());
        return new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                Map.of("scenarioId", "a2a.jsonrpc.probe"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
