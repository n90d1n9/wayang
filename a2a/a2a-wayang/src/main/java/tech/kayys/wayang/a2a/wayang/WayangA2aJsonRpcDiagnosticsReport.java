package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangDiagnosticsReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

/**
 * Compact aggregate diagnostic report for the A2A JSON-RPC HTTP surface.
 */
public record WayangA2aJsonRpcDiagnosticsReport(
        String diagnosticsId,
        boolean passed,
        int exitCode,
        boolean bindingReportPassed,
        boolean routeCatalogRequired,
        boolean routeCatalogPassed,
        boolean smokeRequired,
        boolean smokePassed,
        int issueCount,
        List<Map<String, Object>> checks,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public static final String DIAGNOSTICS_ID = "a2a.jsonrpc.diagnostics";
    public static final String OPERATION_JSON_RPC_DIAGNOSTICS = "JsonRpcDiagnostics";

    public WayangA2aJsonRpcDiagnosticsReport {
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank() ? DIAGNOSTICS_ID : diagnosticsId.trim();
        exitCode = Math.max(0, exitCode);
        checks = copyObjects(checks);
        issues = copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aJsonRpcDiagnosticsReport run(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return WayangA2aJsonRpcSpecHealth.from(resolved).diagnosticsReport();
    }

    public static WayangA2aJsonRpcDiagnosticsReport from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config) {
        return WayangA2aJsonRpcDiagnosticsReportBuilder.from(readiness, config).build();
    }

    public static WayangA2aJsonRpcDiagnosticsReport from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return WayangA2aJsonRpcDiagnosticsReportBuilder.from(readiness, config, specAlignment).build();
    }

    public static WayangA2aJsonRpcDiagnosticsReport fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcDiagnosticsReportProjection.fromMap(values);
    }

    public static WayangA2aJsonRpcDiagnosticsReport fromJson(String json) {
        return fromMap(bodyMap(json));
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcDiagnosticsReportProjection.report(this);
    }

    public WayangDiagnosticsReport standardDiagnostics() {
        return WayangA2aJsonRpcDiagnosticsReportProjection.standardDiagnostics(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcDiagnosticsReportProjection.response(this);
    }
}
