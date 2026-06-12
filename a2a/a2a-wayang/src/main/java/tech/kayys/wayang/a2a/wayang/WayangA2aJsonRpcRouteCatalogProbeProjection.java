package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC route catalog probe envelopes.
 */
final class WayangA2aJsonRpcRouteCatalogProbeProjection {

    private WayangA2aJsonRpcRouteCatalogProbeProjection() {
    }

    static WayangA2aJsonRpcRouteCatalogProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcRouteCatalogProbeResult(
                number(copy.get("statusCode"), 0),
                bool(copy.get("httpSuccessful"), false),
                text(copy.get("routeOperation"), ""),
                text(copy.get("protocolVersion"), ""),
                text(copy.get("contentType"), ""),
                text(copy.get("allow"), ""),
                number(copy.get("routeCount"), 0),
                number(copy.get("enabledRouteCount"), 0),
                bool(copy.get("endpointDescriptor"), false),
                bool(copy.get("smokeDescriptor"), false),
                bool(copy.get("routeCatalogDescriptor"), false),
                bool(copy.get("diagnosticsReportDescriptor"), false),
                bool(copy.get("specComplianceReportDescriptor"), false),
                bool(copy.get("bindingReportDescriptor"), false),
                bool(copy.get("readinessDescriptor"), false),
                bool(copy.get("readinessIssueSummaryDescriptor"), false),
                number(copy.get("issueCount"), 0),
                WayangA2aMaps.objectList(copy.get("issues")),
                child(copy, "body"),
                child(copy, "headers"));
    }

    static Map<String, Object> probe(WayangA2aJsonRpcRouteCatalogProbeResult probe) {
        WayangA2aJsonRpcRouteCatalogProbeResult resolved = Objects.requireNonNull(probe, "probe");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("httpSuccessful", resolved.httpSuccessful());
        values.put("routeOperation", resolved.routeOperation());
        values.put("protocolVersion", resolved.protocolVersion());
        values.put("contentType", resolved.contentType());
        values.put("allow", resolved.allow());
        values.put("routeCatalogRoute", resolved.routeCatalogRoute());
        values.put("jsonContent", resolved.jsonContent());
        values.put("complete", resolved.complete());
        values.put("passed", resolved.passed());
        values.put("issueCount", resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("routeCount", resolved.routeCount());
        values.put("enabledRouteCount", resolved.enabledRouteCount());
        values.put("endpointDescriptor", resolved.endpointDescriptor());
        values.put("smokeDescriptor", resolved.smokeDescriptor());
        values.put("routeCatalogDescriptor", resolved.routeCatalogDescriptor());
        values.put("diagnosticsReportDescriptor", resolved.diagnosticsReportDescriptor());
        values.put("specComplianceReportDescriptor", resolved.specComplianceReportDescriptor());
        values.put("bindingReportDescriptor", resolved.bindingReportDescriptor());
        values.put("readinessDescriptor", resolved.readinessDescriptor());
        values.put("readinessIssueSummaryDescriptor", resolved.readinessIssueSummaryDescriptor());
        values.put("body", resolved.body());
        values.put("headers", resolved.headers());
        return WayangA2aMaps.copyMap(values);
    }
}
