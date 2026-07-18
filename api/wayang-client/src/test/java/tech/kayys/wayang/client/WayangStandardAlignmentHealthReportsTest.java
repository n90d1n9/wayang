package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.alignment.WayangStandardAlignmentHealthReports;
import tech.kayys.wayang.alignment.WayangStandardAlignmentMaps;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderDiagnostics;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderIssue;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentHealthReportsTest {

    @Test
    void configuredComposerCombinesPortfoliosForPinnedKnownStandards() {
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.configured(
                WayangStandardAlignmentPolicyConfig.pinnedKnownStandards(),
                portfolio("a2a"),
                portfolio("a2ui"),
                portfolio("agentic-commerce"));

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("ready");
        assertThat(health.portfolio().standardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(health.policyAssessment().requiredStandardIds())
                .containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(health.registryDrift().driftFree()).isTrue();
    }

    @Test
    void builderCombinesPortfoliosAndReportMaps() {
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.builder()
                .config(WayangStandardAlignmentPolicyConfig.strict("agent2agent", "agent-to-user-interface"))
                .portfolio(portfolio("a2a"))
                .reportMap(report("a2ui"))
                .build();

        assertThat(health.ready()).isTrue();
        assertThat(health.portfolio().standardIds()).containsExactly("a2a", "a2ui");
        assertThat(health.policyAssessment().requiredStandardIds()).containsExactly("a2a", "a2ui");
    }

    @Test
    void builderCarriesProviderDiagnosticsIntoHealth() {
        WayangStandardAlignmentPortfolio portfolio = portfolio("a2a");
        WayangStandardAlignmentProviderDiagnostics diagnostics = diagnostics(portfolio);

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.builder()
                .config(WayangStandardAlignmentPolicyConfig.strict("a2a"))
                .portfolio(portfolio)
                .providerDiagnostics(diagnostics)
                .build();

        assertThat(health.status()).isEqualTo("warning");
        assertThat(health.providerDiagnostics().providerIds()).containsExactly("a2a-provider");
        assertThat(health.providerDiagnostics().issueCount()).isEqualTo(1);
        assertThat(health.recommendations())
                .containsExactly("Review standard-alignment provider broken-provider: boom");
    }

    @Test
    void configuredComposerCarriesProviderDiagnosticsIntoHealth() {
        WayangStandardAlignmentPortfolio portfolio = portfolio("a2a");

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.configured(
                WayangStandardAlignmentPolicyConfig.strict("a2a"),
                diagnostics(portfolio),
                portfolio);

        assertThat(health.providerDiagnostics().providerIds()).containsExactly("a2a-provider");
        assertThat(health.providerDiagnostics().issueCount()).isEqualTo(1);
        assertThat(map(health.toMap().get("providerDiagnostics")))
                .containsEntry("providerCount", 1)
                .containsEntry("issueCount", 1);
    }

    @Test
    void pinnedRegistryComposerReportsMissingSubsetStandards() {
        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReports.pinnedRegistry(
                List.of("a2a", "a2ui"),
                portfolio("a2a"));

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.policyAssessment().missingStandardIds()).containsExactly("a2ui");
        assertThat(health.recommendations())
                .containsExactly("Add alignment report for required standard: a2ui.");
    }

    private static WayangStandardAlignmentPortfolio portfolio(String standardId) {
        return WayangStandardAlignmentPortfolio.fromReportMaps(report(standardId));
    }

    private static WayangStandardAlignmentProviderDiagnostics diagnostics(
            WayangStandardAlignmentPortfolio portfolio) {
        return new WayangStandardAlignmentProviderDiagnostics(
                List.of("a2a-provider"),
                List.of(WayangStandardAlignmentProviderSummary.from(
                        "a2a-provider",
                        "example.A2aProvider",
                        10,
                        portfolio)),
                List.of(new WayangStandardAlignmentProviderIssue(
                        "broken-provider",
                        "example.BrokenProvider",
                        "boom")));
    }

    private static Map<String, Object> report(String standardId) {
        WayangStandardDefinition definition = WayangStandardRegistry.find(standardId).orElseThrow();
        Map<String, Object> standard = new LinkedHashMap<>(definition.toDescriptor().toMap());
        return Map.of(
                "standard", standard,
                "aligned", true,
                "requirementCount", 1,
                "alignedCount", 1,
                "gapCount", 0,
                "gapIds", List.of(),
                "gapCategories", List.of());
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangStandardAlignmentMaps.copy((Map<?, ?>) value);
    }
}
