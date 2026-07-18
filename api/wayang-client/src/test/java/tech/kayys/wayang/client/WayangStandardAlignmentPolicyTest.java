package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicy;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicyAssessment;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPortfolio;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentPolicyTest {

    @Test
    void strictPolicyRequiresStandardsAndBlocksGaps() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()),
                report(
                        "agentic-commerce",
                        false,
                        13,
                        12,
                        1,
                        List.of("route.checkout"),
                        List.of("route")));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.strict(
                "Agent2Agent Protocol",
                "a2ui",
                "agentic-commerce");

        WayangStandardAlignmentPolicyAssessment assessment = policy.assess(portfolio);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.requiredStandardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(assessment.presentStandardIds()).containsExactly("a2a", "agentic-commerce");
        assertThat(assessment.missingStandardIds()).containsExactly("a2ui");
        assertThat(assessment.failingStandardIds()).containsExactly("agentic-commerce");
        assertThat(assessment.warningStandardIds()).isEmpty();
        assertThat(assessment.recommendations())
                .containsExactly(
                        "Add alignment report for required standard: a2ui.",
                        "Resolve blocking alignment gaps for standard: agentic-commerce.");
        assertThat(assessment.toMap())
                .containsEntry("ready", false)
                .containsEntry("missingStandardIds", List.of("a2ui"));
    }

    @Test
    void warningGapCategoriesDoNotBlockReadiness() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report(
                        "a2a",
                        false,
                        20,
                        19,
                        1,
                        List.of("route.tasks.get"),
                        List.of("Route")));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.builder()
                .requiredStandard("agent2agent")
                .warningGapCategory("route")
                .build();

        WayangStandardAlignmentPolicyAssessment assessment = portfolio.assess(policy);

        assertThat(assessment.ready()).isTrue();
        assertThat(assessment.hasFailures()).isFalse();
        assertThat(assessment.hasWarnings()).isTrue();
        assertThat(assessment.missingStandardIds()).isEmpty();
        assertThat(assessment.failingStandardIds()).isEmpty();
        assertThat(assessment.warningStandardIds()).containsExactly("a2a");
        assertThat(assessment.recommendations())
                .containsExactly("Review warning-only alignment gaps for standard: a2a.");
    }

    @Test
    void uncategorizedGapsRemainBlocking() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", false, 20, 19, 1, List.of("route.tasks.get"), List.of()));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.builder()
                .requiredStandard("a2a")
                .warningGapCategory("route")
                .build();

        WayangStandardAlignmentPolicyAssessment assessment = policy.assess(portfolio);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.failingStandardIds()).containsExactly("a2a");
        assertThat(assessment.warningStandardIds()).isEmpty();
    }

    @Test
    void versionRequirementsBlockMismatchedPresentStandards() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2ui", "v0.7", true, 11, 11, 0, List.of(), List.of()));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.builder()
                .requiredStandardVersion("agent-to-user-interface", "v0.8")
                .build();

        WayangStandardAlignmentPolicyAssessment assessment = policy.assess(portfolio);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.hasFailures()).isTrue();
        assertThat(assessment.requiredStandardIds()).containsExactly("a2ui");
        assertThat(assessment.requiredVersions()).containsEntry("a2ui", "v0.8");
        assertThat(assessment.actualVersions()).containsEntry("a2ui", "v0.7");
        assertThat(assessment.versionMismatchStandardIds()).containsExactly("a2ui");
        assertThat(assessment.recommendations())
                .containsExactly("Update alignment report for standard a2ui to required version v0.8.");
        assertThat(assessment.toMap())
                .containsEntry("requiredVersions", Map.of("a2ui", "v0.8"))
                .containsEntry("actualVersions", Map.of("a2ui", "v0.7"))
                .containsEntry("versionMismatchStandardIds", List.of("a2ui"));
    }

    @Test
    void versionRequirementsImplyRequiredStandards() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.builder().build();
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.builder()
                .requiredStandardVersion("a2ui", "v0.8")
                .build();

        WayangStandardAlignmentPolicyAssessment assessment = policy.assess(portfolio);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.requiredStandardIds()).containsExactly("a2ui");
        assertThat(assessment.missingStandardIds()).containsExactly("a2ui");
        assertThat(assessment.actualVersions()).isEmpty();
        assertThat(assessment.versionMismatchStandardIds()).isEmpty();
    }

    @Test
    void nullPolicyUsesDefaultGapBlockingPolicy() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", false, 20, 19, 1, List.of("route.tasks.get"), List.of("route")));

        WayangStandardAlignmentPolicyAssessment assessment = portfolio.assess(null);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.requiredStandardIds()).isEmpty();
        assertThat(assessment.failingStandardIds()).containsExactly("a2a");
    }

    private static Map<String, Object> report(
            String standardId,
            boolean aligned,
            int requirementCount,
            int alignedCount,
            int gapCount,
            List<String> gapIds,
            List<String> gapCategories) {
        return report(standardId, "1.0", aligned, requirementCount, alignedCount, gapCount, gapIds, gapCategories);
    }

    private static Map<String, Object> report(
            String standardId,
            String version,
            boolean aligned,
            int requirementCount,
            int alignedCount,
            int gapCount,
            List<String> gapIds,
            List<String> gapCategories) {
        return Map.of(
                "standard", Map.of(
                        "standardId", standardId,
                        "name", standardId,
                        "version", version,
                        "binding", "JSONRPC",
                        "specUrl", "https://example.test/" + standardId),
                "aligned", aligned,
                "requirementCount", requirementCount,
                "alignedCount", alignedCount,
                "gapCount", gapCount,
                "gapIds", gapIds,
                "gapCategories", gapCategories);
    }
}
