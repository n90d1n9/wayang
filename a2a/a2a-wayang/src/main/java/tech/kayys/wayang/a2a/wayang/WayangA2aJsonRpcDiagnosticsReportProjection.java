package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangDiagnosticsReport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC diagnostics reports.
 */
final class WayangA2aJsonRpcDiagnosticsReportProjection {

    private WayangA2aJsonRpcDiagnosticsReportProjection() {
    }

    static WayangA2aJsonRpcDiagnosticsReport fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcDiagnosticsReport(
                text(copy.get("diagnosticsId"), WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID),
                bool(copy.get("passed"), false),
                number(copy.get("exitCode"), WayangA2aJsonRpcSmokeResult.EXIT_FAILURE),
                bool(copy.get("bindingReportPassed"), false),
                bool(copy.get("routeCatalogRequired"), false),
                bool(copy.get("routeCatalogPassed"), false),
                bool(copy.get("smokeRequired"), false),
                bool(copy.get("smokePassed"), false),
                number(copy.get("issueCount"), 0),
                WayangA2aMaps.objectList(copy.get("checks")),
                WayangA2aMaps.objectList(copy.get("issues")),
                child(copy, "attributes"));
    }

    static Map<String, Object> report(WayangA2aJsonRpcDiagnosticsReport report) {
        WayangA2aJsonRpcDiagnosticsReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put("passed", resolved.passed());
        values.put("exitCode", resolved.exitCode());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("routeCatalogRequired", resolved.routeCatalogRequired());
        values.put("routeCatalogPassed", resolved.routeCatalogPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put("issueCount", resolved.issueCount());
        values.put("checks", resolved.checks());
        values.put("issues", resolved.issues());
        values.put("attributes", resolved.attributes());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangDiagnosticsReport standardDiagnostics(WayangA2aJsonRpcDiagnosticsReport report) {
        WayangA2aJsonRpcDiagnosticsReport resolved = Objects.requireNonNull(report, "report");
        return WayangDiagnosticsReport.from(
                resolved.diagnosticsId(),
                resolved.passed(),
                resolved.exitCode(),
                resolved.issueCount(),
                resolved.checks(),
                resolved.issues(),
                resolved.attributes());
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcDiagnosticsReport report) {
        WayangA2aJsonRpcDiagnosticsReport resolved = Objects.requireNonNull(report, "report");
        return WayangA2aJsonRpcHttpResponses.json(
                WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
                resolved.toJson());
    }
}
