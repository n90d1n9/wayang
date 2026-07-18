package tech.kayys.wayang.readiness;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.alignment.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for standard alignment health checks.
 * Evaluates whether standards alignment metrics meet readiness criteria.
 */
public class StandardAlignmentAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.standard-alignment.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return "standards";
    }

    @Override
    protected String buildProbeName() {
        return "standards.alignment";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangStandardAlignmentHealthReport health = (WayangStandardAlignmentHealthReport) input;
        WayangStandardAlignmentHealthReport resolved = health == null
                ? WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                        WayangStandardAlignmentPortfolio.builder().build(),
                        WayangStandardAlignmentPolicyConfig.none())
                : health;

        if (resolved.ready()) {
            return List.of();
        }

        return List.of(WayangReadinessReports.issue(
                "standard_alignment_not_ready",
                getSource(),
                "Standard alignment health is not ready.",
                WayangReadinessAttributeMaps.ordered(
                        "status", resolved.status(),
                        "blocked", resolved.blocked(),
                        "warning", resolved.warning(),
                        "recommendations", resolved.recommendations())));
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangStandardAlignmentHealthReport health = (WayangStandardAlignmentHealthReport) input;
        WayangStandardAlignmentHealthReport resolved = health == null
                ? WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                        WayangStandardAlignmentPortfolio.builder().build(),
                        WayangStandardAlignmentPolicyConfig.none())
                : health;

        return WayangReadinessAttributeMaps.ordered(
                "status", resolved.status(),
                "blocked", resolved.blocked(),
                "warning", resolved.warning(),
                "recommendations", resolved.recommendations());
    }

    @Override
    protected List<Map<String, Object>> buildProbes(Object input, List<Map<String, Object>> issues) {
        WayangStandardAlignmentHealthReport health = (WayangStandardAlignmentHealthReport) input;
        WayangStandardAlignmentHealthReport resolved = health == null
                ? WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                        WayangStandardAlignmentPortfolio.builder().build(),
                        WayangStandardAlignmentPolicyConfig.none())
                : health;

        boolean ready = issues.isEmpty();
        return List.of(WayangReadinessReports.probe(
                buildProbeName(),
                true,
                ready,
                issues.size(),
                WayangReadinessAttributeMaps.ordered(
                        "status", resolved.status(),
                        "blocked", resolved.blocked(),
                        "warning", resolved.warning(),
                        "providerCount", resolved.providerDiagnostics().providerCount(),
                        "providerIssueCount", resolved.providerDiagnostics().issueCount())));
    }
}
