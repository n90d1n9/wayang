package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

/**
 * Compact operator-facing issue summary for A2A JSON-RPC readiness probes.
 */
public record WayangA2aJsonRpcReadinessIssueSummary(
        boolean passed,
        int exitCode,
        boolean bindingReportPassed,
        boolean routeCatalogRequired,
        boolean routeCatalogPassed,
        boolean smokeRequired,
        boolean smokePassed,
        int issueCount,
        int readinessIssueCount,
        int bindingReportIssueCount,
        int diagnosticHandlerIssueCount,
        int methodDispatchIssueCount,
        int routeCatalogIssueCount,
        int smokeIssueCount,
        List<Map<String, Object>> issues) {

    public static final String OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY = "JsonRpcReadinessIssueSummary";

    public WayangA2aJsonRpcReadinessIssueSummary {
        exitCode = Math.max(0, exitCode);
        readinessIssueCount = Math.max(0, readinessIssueCount);
        bindingReportIssueCount = Math.max(0, bindingReportIssueCount);
        diagnosticHandlerIssueCount = Math.max(0, diagnosticHandlerIssueCount);
        methodDispatchIssueCount = Math.max(0, methodDispatchIssueCount);
        routeCatalogIssueCount = Math.max(0, routeCatalogIssueCount);
        smokeIssueCount = Math.max(0, smokeIssueCount);
        issues = copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
    }

    public static WayangA2aJsonRpcReadinessIssueSummary from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness).build();
    }

    public static WayangA2aJsonRpcReadinessIssueSummary from(WayangA2aHttpResponse response) {
        return fromJson(Objects.requireNonNull(response, "response").body());
    }

    public static WayangA2aJsonRpcReadinessIssueSummary fromJson(String json) {
        return fromMap(bodyMap(json));
    }

    public static WayangA2aJsonRpcReadinessIssueSummary fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcReadinessIssueSummaryProjection.fromMap(values);
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcReadinessIssueSummaryProjection.summary(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcReadinessIssueSummaryProjection.response(this);
    }
}
