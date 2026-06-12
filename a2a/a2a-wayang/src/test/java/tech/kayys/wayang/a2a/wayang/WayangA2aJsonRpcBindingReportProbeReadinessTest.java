package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProbeReadinessTest {

    @Test
    void summarizesPassingLegacyBindingReportProbe() {
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(legacyBindingReportResponse());

        WayangA2aJsonRpcBindingReportProbeReadiness readiness =
                WayangA2aJsonRpcBindingReportProbeReadiness.from(probe);

        assertThat(readiness.bindingReportRoute()).isTrue();
        assertThat(readiness.jsonContent()).isTrue();
        assertThat(readiness.complete()).isTrue();
        assertThat(readiness.passed()).isTrue();
        assertThat(probe.bindingReportRoute()).isEqualTo(readiness.bindingReportRoute());
        assertThat(probe.jsonContent()).isEqualTo(readiness.jsonContent());
        assertThat(probe.complete()).isEqualTo(readiness.complete());
        assertThat(probe.passed()).isEqualTo(readiness.passed());
    }

    @Test
    void treatsReportedIncompleteMethodDispatchAsIncomplete() {
        Map<String, Object> body = new LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
        body.put("methodDispatch", WayangA2aJsonRpcMethodDispatchCoverage.from(
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod")).toMap());
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(bindingReportResponse(body));

        WayangA2aJsonRpcBindingReportProbeReadiness readiness =
                WayangA2aJsonRpcBindingReportProbeReadiness.from(probe);

        assertThat(readiness.bindingReportRoute()).isTrue();
        assertThat(readiness.jsonContent()).isTrue();
        assertThat(readiness.complete()).isFalse();
        assertThat(readiness.passed()).isFalse();
    }

    @Test
    void requiresSuccessfulHttpRouteAndJsonContentBeforePassing() {
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(new WayangA2aHttpResponse(
                        200,
                        "text/plain",
                        WayangA2aJsonRpcBindingReport.defaults().toJson(),
                        Map.of(
                                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                                "text/plain",
                                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                                "WrongRoute")));

        WayangA2aJsonRpcBindingReportProbeReadiness readiness =
                WayangA2aJsonRpcBindingReportProbeReadiness.from(probe);

        assertThat(readiness.bindingReportRoute()).isFalse();
        assertThat(readiness.jsonContent()).isFalse();
        assertThat(readiness.complete()).isTrue();
        assertThat(readiness.passed()).isFalse();
    }

    private static WayangA2aHttpResponse legacyBindingReportResponse() {
        return WayangA2aJsonRpcBindingReport.defaults().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }

    private static WayangA2aHttpResponse bindingReportResponse(Map<String, Object> body) {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(body),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_A2A_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }
}
