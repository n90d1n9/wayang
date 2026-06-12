package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC spec compliance reports.
 */
final class WayangA2aJsonRpcSpecComplianceReportProjection {

    private WayangA2aJsonRpcSpecComplianceReportProjection() {
    }

    static WayangA2aJsonRpcSpecComplianceReport fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcSpecComplianceReport(
                text(copy.get("complianceId"), WayangA2aJsonRpcSpecComplianceReport.COMPLIANCE_ID),
                text(copy.get("specUrl"), WayangA2aJsonRpcSpecComplianceReport.SPEC_URL),
                text(copy.get("protocolVersion"), A2aProtocol.VERSION),
                text(copy.get("binding"), A2aProtocol.BINDING_JSONRPC),
                bool(copy.get("passed"), false),
                number(copy.get("operationCount"), 0),
                number(copy.get("supportedOperationCount"), 0),
                number(copy.get("streamingOperationCount"), 0),
                bool(copy.get("endpointPublished"), false),
                text(copy.get("endpointPath"), ""),
                number(copy.get("issueCount"), 0),
                WayangA2aMaps.objectList(copy.get("issues")),
                WayangA2aMaps.objectList(copy.get("operations")),
                child(copy, "publication"),
                child(copy, "attributes"));
    }

    static Map<String, Object> attributes(WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aSpecAlignmentSnapshot resolved = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("methodMappingSection", "5.3");
        values.put("jsonRpcSection", "9");
        values.put("source", "A2aHttpRouteCatalog.standard");
        values.put("specAlignment", resolved.toMap());
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> report(WayangA2aJsonRpcSpecComplianceReport report) {
        WayangA2aJsonRpcSpecComplianceReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("complianceId", resolved.complianceId());
        values.put("specUrl", resolved.specUrl());
        values.put("protocolVersion", resolved.protocolVersion());
        values.put("binding", resolved.binding());
        values.put("passed", resolved.passed());
        values.put("operationCount", resolved.operationCount());
        values.put("supportedOperationCount", resolved.supportedOperationCount());
        values.put("streamingOperationCount", resolved.streamingOperationCount());
        values.put("endpointPublished", resolved.endpointPublished());
        values.put("endpointPath", resolved.endpointPath());
        values.put("issueCount", resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("operations", resolved.operations());
        values.put("publication", resolved.publication());
        values.put("attributes", resolved.attributes());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcSpecComplianceReport report) {
        WayangA2aJsonRpcSpecComplianceReport resolved = Objects.requireNonNull(report, "report");
        return WayangA2aJsonRpcHttpResponses.json(
                WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE,
                resolved.toJson());
    }
}
