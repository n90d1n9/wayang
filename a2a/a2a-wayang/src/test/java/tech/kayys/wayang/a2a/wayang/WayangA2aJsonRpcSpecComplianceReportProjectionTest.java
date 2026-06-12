package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSpecComplianceReportProjectionTest {

    @Test
    void keepsOrderedComplianceEnvelope() {
        WayangA2aJsonRpcSpecComplianceReport report = report();

        Map<String, Object> values = WayangA2aJsonRpcSpecComplianceReportProjection.report(report);

        assertThat(values.keySet()).containsExactly(
                "complianceId",
                "specUrl",
                "protocolVersion",
                "binding",
                "passed",
                "operationCount",
                "supportedOperationCount",
                "streamingOperationCount",
                "endpointPublished",
                "endpointPath",
                "issueCount",
                "issues",
                "operations",
                "publication",
                "attributes");
        assertThat(values)
                .containsEntry("complianceId", WayangA2aJsonRpcSpecComplianceReport.COMPLIANCE_ID)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("endpointPath", "/a2a");
        assertThat(map(values.get("attributes")).keySet()).containsExactly(
                "methodMappingSection",
                "jsonRpcSection",
                "source",
                "specAlignment");
    }

    @Test
    void parsesComplianceMapsWithDefaultsAndNestedObjects() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", true);
        values.put("operationCount", 11);
        values.put("supportedOperationCount", 11);
        values.put("streamingOperationCount", 2);
        values.put("endpointPublished", true);
        values.put("endpointPath", "/a2a");
        values.put("issues", List.of());
        values.put("operations", List.of(Map.of("operation", A2aProtocol.OPERATION_SEND_MESSAGE)));
        values.put("publication", Map.of("routeCount", 8));
        values.put("attributes", WayangA2aJsonRpcSpecComplianceReportProjection.attributes(
                WayangA2aSpecAlignmentSnapshot.defaults()));

        WayangA2aJsonRpcSpecComplianceReport report =
                WayangA2aJsonRpcSpecComplianceReportProjection.fromMap(values);

        assertThat(report.complianceId()).isEqualTo(WayangA2aJsonRpcSpecComplianceReport.COMPLIANCE_ID);
        assertThat(report.specUrl()).isEqualTo(WayangA2aJsonRpcSpecComplianceReport.SPEC_URL);
        assertThat(report.protocolVersion()).isEqualTo(A2aProtocol.VERSION);
        assertThat(report.binding()).isEqualTo(A2aProtocol.BINDING_JSONRPC);
        assertThat(report.operations()).singleElement()
                .satisfies(operation -> assertThat(operation)
                        .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE));
        assertThat(map(report.publication())).containsEntry("routeCount", 8);
    }

    @Test
    void buildsOrderedComplianceAttributes() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                19,
                1,
                List.of("route.SendMessage"));

        Map<String, Object> attributes =
                WayangA2aJsonRpcSpecComplianceReportProjection.attributes(specAlignment);

        assertThat(attributes.keySet()).containsExactly(
                "methodMappingSection",
                "jsonRpcSection",
                "source",
                "specAlignment");
        assertThat(attributes)
                .containsEntry("methodMappingSection", "5.3")
                .containsEntry("jsonRpcSection", "9")
                .containsEntry("source", "A2aHttpRouteCatalog.standard");
        assertThat(map(attributes.get("specAlignment")))
                .containsEntry("aligned", false)
                .containsEntry("gapCount", 1);
    }

    @Test
    void buildsComplianceResponseThroughProjection() {
        WayangA2aJsonRpcSpecComplianceReport report = report();

        WayangA2aHttpResponse response = WayangA2aJsonRpcSpecComplianceReportProjection.response(report);

        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE);
        assertThat(WayangA2aJsonRpcSpecComplianceReport.fromJson(response.body()).toJson())
                .isEqualTo(report.toJson());
    }

    private static WayangA2aJsonRpcSpecComplianceReport report() {
        return new WayangA2aJsonRpcSpecComplianceReport(
                WayangA2aJsonRpcSpecComplianceReport.COMPLIANCE_ID,
                WayangA2aJsonRpcSpecComplianceReport.SPEC_URL,
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                true,
                11,
                11,
                2,
                true,
                "/a2a",
                0,
                List.of(),
                List.of(Map.of("operation", A2aProtocol.OPERATION_SEND_MESSAGE)),
                Map.of("routeCount", 8),
                WayangA2aJsonRpcSpecComplianceReportProjection.attributes(
                        WayangA2aSpecAlignmentSnapshot.defaults()));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
