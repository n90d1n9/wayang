package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentProviderPolicyTest {

    @Test
    void defaultPolicyWarnsOnProviderIssuesWithoutBlocking() {
        WayangStandardAlignmentProviderPolicyAssessment assessment =
                WayangStandardAlignmentProviderPolicy.defaultPolicy().assess(diagnosticsWithIssue());

        assertThat(assessment.ready()).isTrue();
        assertThat(assessment.blocked()).isFalse();
        assertThat(assessment.warning()).isTrue();
        assertThat(assessment.issueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.WARN);
        assertThat(assessment.recommendations())
                .containsExactly("Review standard-alignment provider broken-provider: boom");
    }

    @Test
    void blockModeBlocksProviderIssues() {
        WayangStandardAlignmentProviderPolicy policy = new WayangStandardAlignmentProviderPolicy(
                List.of(),
                0,
                WayangStandardAlignmentProviderIssueMode.BLOCK);

        WayangStandardAlignmentProviderPolicyAssessment assessment = policy.assess(diagnosticsWithIssue());

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.blocked()).isTrue();
        assertThat(assessment.warning()).isFalse();
        assertThat(assessment.recommendations())
                .containsExactly("Review standard-alignment provider broken-provider: boom");
    }

    @Test
    void ignoreModeIgnoresProviderIssueReadinessImpact() {
        WayangStandardAlignmentProviderPolicy policy = new WayangStandardAlignmentProviderPolicy(
                List.of(),
                0,
                WayangStandardAlignmentProviderIssueMode.IGNORE);

        WayangStandardAlignmentProviderPolicyAssessment assessment = policy.assess(diagnosticsWithIssue());

        assertThat(assessment.ready()).isTrue();
        assertThat(assessment.blocked()).isFalse();
        assertThat(assessment.warning()).isFalse();
        assertThat(assessment.recommendations()).isEmpty();
    }

    @Test
    void requiredProviderAndMinimumCountBlockWhenMissing() {
        WayangStandardAlignmentProviderPolicy policy = new WayangStandardAlignmentProviderPolicy(
                List.of("a2a-provider", "a2ui-provider"),
                2,
                WayangStandardAlignmentProviderIssueMode.WARN);

        WayangStandardAlignmentProviderPolicyAssessment assessment = policy.assess(diagnostics("a2a-provider"));

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.blocked()).isTrue();
        assertThat(assessment.hasProviderRequirements()).isTrue();
        assertThat(assessment.missingProviderIds()).containsExactly("a2ui-provider");
        assertThat(assessment.recommendations())
                .containsExactly(
                        "Register at least 2 standard-alignment provider(s).",
                        "Register required standard-alignment provider: a2ui-provider.");
    }

    private static WayangStandardAlignmentProviderDiagnostics diagnosticsWithIssue() {
        return new WayangStandardAlignmentProviderDiagnostics(
                List.of("a2a-provider"),
                List.of(provider("a2a-provider")),
                List.of(new WayangStandardAlignmentProviderIssue(
                        "broken-provider",
                        "example.BrokenProvider",
                        "boom")));
    }

    private static WayangStandardAlignmentProviderDiagnostics diagnostics(String providerId) {
        return new WayangStandardAlignmentProviderDiagnostics(
                List.of(providerId),
                List.of(provider(providerId)),
                List.of());
    }

    private static WayangStandardAlignmentProviderSummary provider(String providerId) {
        return WayangStandardAlignmentProviderSummary.from(
                providerId,
                "example.Provider",
                10,
                WayangStandardAlignmentPortfolio.fromReportMaps(report()));
    }

    private static Map<String, Object> report() {
        WayangStandardDefinition definition = WayangStandardRegistry.find("a2a").orElseThrow();
        return Map.of(
                "standard",
                definition.toDescriptor().toMap(),
                "aligned",
                true,
                "requirementCount",
                1,
                "alignedCount",
                1,
                "gapCount",
                0);
    }
}
