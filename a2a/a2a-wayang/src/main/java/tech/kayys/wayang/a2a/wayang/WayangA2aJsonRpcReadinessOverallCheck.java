package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcReadinessOverallCheck(
        WayangA2aJsonRpcReadinessProbeResult readiness,
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {

    WayangA2aJsonRpcReadinessOverallCheck {
        readiness = Objects.requireNonNull(readiness, "readiness");
        breakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness)
                : breakdown;
    }

    static WayangA2aJsonRpcReadinessOverallCheck from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {
        return new WayangA2aJsonRpcReadinessOverallCheck(readiness, breakdown);
    }

    WayangA2aJsonRpcReadinessProbeCheck check() {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_READINESS,
                true,
                readiness.passed(),
                0,
                WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS,
                breakdown.readinessIssueCount(),
                "",
                "");
    }

    Map<String, Object> toMap() {
        return check().toMap();
    }
}
