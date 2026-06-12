package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProjectionTest {

    @Test
    void keepsOrderedReportEnvelopeWithoutMethodDispatch() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .build();
        WayangA2aJsonRpcBindingReport report = WayangA2aJsonRpcBindingReport.fromConfig(config);

        Map<String, Object> values = WayangA2aJsonRpcBindingReportProjection.report(report);

        assertThat(values.keySet()).containsExactly(
                "binding",
                "protocolVersion",
                "endpoint",
                "smoke",
                "routeCatalog",
                "diagnosticsReport",
                "specComplianceReport",
                "bindingReport",
                "readiness",
                "readinessIssueSummary",
                "diagnosticHandlers",
                "methodCount",
                "methods",
                "streamingMethods",
                "config");
        assertThat(values)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size())
                .doesNotContainKey("methodDispatch");
        assertThat(map(values.get("endpoint"))).containsEntry("path", "/a2a/rpc");
        assertThat(map(values.get("smoke")))
                .containsEntry("path", "/internal/a2a/smoke")
                .containsEntry("enabled", false);
        assertThat(WayangA2aMaps.objectList(values.get("methods")).getFirst())
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("httpMethod", "POST");
    }

    @Test
    void insertsMethodDispatchBeforeConfigWhenReported() {
        WayangA2aJsonRpcBindingReport report = reportWithCompleteMethodDispatch();

        Map<String, Object> values = WayangA2aJsonRpcBindingReportProjection.report(report);

        assertThat(values.keySet()).containsExactly(
                "binding",
                "protocolVersion",
                "endpoint",
                "smoke",
                "routeCatalog",
                "diagnosticsReport",
                "specComplianceReport",
                "bindingReport",
                "readiness",
                "readinessIssueSummary",
                "diagnosticHandlers",
                "methodCount",
                "methods",
                "streamingMethods",
                "methodDispatch",
                "config");
        assertThat(map(values.get("methodDispatch")))
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
    }

    @Test
    void buildsBindingReportResponseThroughProjection() {
        WayangA2aJsonRpcBindingReport report = WayangA2aJsonRpcBindingReport.defaults();

        WayangA2aHttpResponse response = WayangA2aJsonRpcBindingReportProjection.response(report);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION);
        assertThat(response.body()).isEqualTo(report.toJson());
        assertThat(report.response().body()).isEqualTo(response.body());
    }

    @Test
    void recordDelegatesToProjectionForReportMap() {
        WayangA2aJsonRpcBindingReport report = reportWithCompleteMethodDispatch();

        assertThat(report.toMap()).isEqualTo(WayangA2aJsonRpcBindingReportProjection.report(report));
    }

    private static WayangA2aJsonRpcBindingReport reportWithCompleteMethodDispatch() {
        List<String> methods = WayangA2aJsonRpcMethods.methods();
        return new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                methods,
                WayangA2aJsonRpcMethodDispatchCoverage.from(methods, methods));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
