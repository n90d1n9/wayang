package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicies;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicy;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicyAssessment;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPortfolio;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentPoliciesTest {

    @Test
    void pinnedRegistryUsesRegistryVersionsForKnownStandards() {
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicies.pinnedRegistry(
                "agent2agent",
                "agent-to-user-interface");

        assertThat(policy.requiredStandardIds()).containsExactly("a2a", "a2ui");
        assertThat(policy.requiredVersions())
                .containsEntry("a2a", "1.0")
                .containsEntry("a2ui", "v0.8");
    }

    @Test
    void pinnedRegistryDefaultsToAllKnownStandards() {
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicies.pinnedRegistry();

        assertThat(policy.requiredStandardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(policy.requiredVersions())
                .containsEntry("a2a", "1.0")
                .containsEntry("a2ui", "v0.8")
                .containsEntry("agentic-commerce", "2026-01-30");
    }

    @Test
    void pinnedRegistryFallsBackToRequiredStandardForUnknownIds() {
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicies.pinnedRegistry("custom-standard");

        assertThat(policy.requiredStandardIds()).containsExactly("custom-standard");
        assertThat(policy.requiredVersions()).isEmpty();
    }

    @Test
    void pinnedRegistryPolicyBlocksVersionMismatchThroughAssessment() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(Map.of(
                "standard", Map.of(
                        "standardId", "a2ui",
                        "name", "Agent-to-User Interface",
                        "version", "v0.7",
                        "binding", "HTTP",
                        "specUrl", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json"),
                "aligned", true,
                "requirementCount", 12,
                "alignedCount", 12,
                "gapCount", 0,
                "gapIds", List.of(),
                "gapCategories", List.of()));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicies.pinnedRegistry("a2ui");

        WayangStandardAlignmentPolicyAssessment assessment = policy.assess(portfolio);

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.requiredVersions()).containsEntry("a2ui", "v0.8");
        assertThat(assessment.actualVersions()).containsEntry("a2ui", "v0.7");
        assertThat(assessment.versionMismatchStandardIds()).containsExactly("a2ui");
    }
}
