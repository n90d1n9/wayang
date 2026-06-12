package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public final class WayangStandardAlignmentReadiness {

    public static final String READINESS_ID = "wayang.standard-alignment.readiness";

    private WayangStandardAlignmentReadiness() {
    }

    public static WayangReadinessReport assess(WayangStandardAlignmentHealthReport health) {
        WayangStandardAlignmentHealthReport resolved = health == null
                ? WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                        WayangStandardAlignmentPortfolio.builder().build(),
                        WayangStandardAlignmentPolicyConfig.none())
                : health;
        boolean ready = resolved.ready();
        List<Map<String, Object>> issues = ready
                ? List.of()
                : List.of(WayangReadinessReports.issue(
                        "standard_alignment_not_ready",
                        "standards",
                        "Standard alignment health is not ready.",
                        WayangReadinessAttributeMaps.ordered(
                                "status", resolved.status(),
                                "blocked", resolved.blocked(),
                                "warning", resolved.warning(),
                                "recommendations", resolved.recommendations())));
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "standards.alignment",
                        true,
                        ready,
                        issues.size(),
                        probeAttributes(resolved))),
                issues,
                attributes(resolved));
    }

    private static Map<String, Object> attributes(WayangStandardAlignmentHealthReport report) {
        return WayangReadinessAttributeMaps.ordered(
                "status", report.status(),
                "blocked", report.blocked(),
                "warning", report.warning(),
                "recommendations", report.recommendations());
    }

    private static Map<String, Object> probeAttributes(WayangStandardAlignmentHealthReport report) {
        return WayangReadinessAttributeMaps.ordered(
                "status", report.status(),
                "blocked", report.blocked(),
                "warning", report.warning(),
                "providerCount", report.providerDiagnostics().providerCount(),
                "providerIssueCount", report.providerDiagnostics().issueCount());
    }
}
