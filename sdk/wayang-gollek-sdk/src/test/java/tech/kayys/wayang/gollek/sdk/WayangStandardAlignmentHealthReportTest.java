package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentHealthReportTest {

    @Test
    void createsReadyHealthPayloadFromPortfolioAndPolicy() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()),
                report("a2ui", true, 11, 11, 0, List.of(), List.of()));
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.strict("agent2agent", "a2ui");

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.from(portfolio, policy);

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("ready");
        assertThat(health.blocked()).isFalse();
        assertThat(health.warning()).isFalse();
        assertThat(health.recommendations()).isEmpty();
        assertThat(health.toMap())
                .containsEntry("reportId", WayangStandardAlignmentHealthReport.DEFAULT_REPORT_ID)
                .containsEntry("status", "ready")
                .containsEntry("ready", true)
                .containsEntry("standardIds", List.of("a2a", "a2ui"));
        assertThat(map(health.toMap().get("registryDrift")))
                .containsEntry("checkedStandardIds", List.of("a2a", "a2ui"));
        assertThat(map(health.toMap().get("policyAssessment")))
                .containsEntry("ready", true)
                .containsEntry("requiredStandardIds", List.of("a2a", "a2ui"));
    }

    @Test
    void marksWarningHealthWhenOnlyWarningGapCategoriesExist() {
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.builder()
                .requiredStandard("a2a")
                .warningGapCategory("route")
                .build();

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromReportMaps(
                policy,
                report("a2a", false, 20, 19, 1, List.of("route.tasks.get"), List.of("route")));

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("warning");
        assertThat(health.blocked()).isFalse();
        assertThat(health.warning()).isTrue();
        assertThat(health.recommendations())
                .containsExactly("Review warning-only alignment gaps for standard: a2a.");
        assertThat(health.toMap())
                .containsEntry("aligned", false)
                .containsEntry("gapStandardIds", List.of("a2a"));
    }

    @Test
    void marksBlockedHealthWhenRequiredStandardsAreMissingOrFailing() {
        WayangStandardAlignmentPolicy policy = WayangStandardAlignmentPolicy.strict("a2a", "a2ui");

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromReportMaps(
                policy,
                report("a2a", false, 20, 19, 1, List.of("route.tasks.get"), List.of("route")));

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.blocked()).isTrue();
        assertThat(health.warning()).isFalse();
        assertThat(health.policyAssessment().missingStandardIds()).containsExactly("a2ui");
        assertThat(health.policyAssessment().failingStandardIds()).containsExactly("a2a");
        assertThat(health.recommendations())
                .containsExactly(
                        "Add alignment report for required standard: a2ui.",
                        "Resolve blocking alignment gaps for standard: a2a.");
    }

    @Test
    void pinnedRegistryHealthBlocksMismatchedStandardVersions() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2ui", "v0.7", true, 11, 11, 0, List.of(), List.of()));

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromPinnedRegistry(
                portfolio,
                "agent-to-user-interface");

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.policyAssessment().requiredStandardIds()).containsExactly("a2ui");
        assertThat(health.policyAssessment().requiredVersions()).containsEntry("a2ui", "v0.8");
        assertThat(health.policyAssessment().actualVersions()).containsEntry("a2ui", "v0.7");
        assertThat(health.recommendations())
                .containsExactly("Update alignment report for standard a2ui to required version v0.8.");
    }

    @Test
    void pinnedKnownStandardsHealthRequiresAllRegistryStandards() {
        WayangStandardAlignmentHealthReport health =
                WayangStandardAlignmentHealthReport.fromReportMapsPinnedKnownStandards(
                        report("a2a", "1.0", true, 20, 20, 0, List.of(), List.of()));

        assertThat(health.ready()).isFalse();
        assertThat(health.policyAssessment().requiredStandardIds())
                .containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(health.policyAssessment().missingStandardIds())
                .containsExactly("a2ui", "agentic-commerce");
        assertThat(health.policyAssessment().requiredVersions())
                .containsEntry("a2a", "1.0")
                .containsEntry("a2ui", "v0.8")
                .containsEntry("agentic-commerce", "2026-01-30");
    }

    @Test
    void configuredPolicyHealthUsesDeploymentConfig() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.fromMap(Map.of(
                "mode", "pinned-registry",
                "standardIds", List.of("a2ui")));

        WayangStandardAlignmentHealthReport health =
                WayangStandardAlignmentHealthReport.fromReportMapsConfiguredPolicy(
                        config,
                        report("a2ui", "v0.7", true, 11, 11, 0, List.of(), List.of()));

        assertThat(health.ready()).isFalse();
        assertThat(health.policyAssessment().requiredVersions()).containsEntry("a2ui", "v0.8");
        assertThat(health.policyAssessment().actualVersions()).containsEntry("a2ui", "v0.7");
    }

    @Test
    void configuredHealthCanWarnOnRegistryDriftWithoutBlocking() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.builder()
                .mode("strict")
                .standardId("a2ui")
                .registryDriftMode(WayangStandardRegistryDriftMode.WARN)
                .build();

        WayangStandardAlignmentHealthReport health =
                WayangStandardAlignmentHealthReport.fromReportMapsConfiguredPolicy(
                        config,
                        report("a2ui", "v0.8", true, 11, 11, 0, List.of(), List.of()));

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("warning");
        assertThat(health.warning()).isTrue();
        assertThat(health.blocked()).isFalse();
        assertThat(health.registryDrift().hasDrift()).isTrue();
        assertThat(health.toMap()).containsEntry("registryDriftMode", "warn");
        assertThat(health.recommendations())
                .contains("Review registry drift for standard a2ui field binding.");
    }

    @Test
    void configuredHealthCanBlockOnRegistryDrift() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.fromMap(Map.of(
                "mode", "strict",
                "standardIds", List.of("a2ui"),
                "registryDriftMode", "block"));

        WayangStandardAlignmentHealthReport health =
                WayangStandardAlignmentHealthReport.fromReportMapsConfiguredPolicy(
                        config,
                        report("a2ui", "v0.8", true, 11, 11, 0, List.of(), List.of()));

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.warning()).isFalse();
        assertThat(health.blocked()).isTrue();
        assertThat(health.registryDriftMode()).isEqualTo(WayangStandardRegistryDriftMode.BLOCK);
        assertThat(health.recommendations())
                .contains("Resolve registry drift for standard a2ui field specUrl.");
    }

    @Test
    void providerIssuesAreSurfacedAsWarningsWithoutBlockingReadyHealth() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()));
        WayangStandardAlignmentProviderSummary providerSummary = WayangStandardAlignmentProviderSummary.from(
                "a2a-provider",
                "example.A2aProvider",
                10,
                portfolio);
        WayangStandardAlignmentProviderIssue issue = new WayangStandardAlignmentProviderIssue(
                "broken-provider",
                "example.BrokenProvider",
                "boom");
        WayangStandardAlignmentProviderDiagnostics providerDiagnostics =
                new WayangStandardAlignmentProviderDiagnostics(
                        List.of("a2a-provider"),
                        List.of(providerSummary),
                        List.of(issue));

        WayangStandardAlignmentHealthReport health =
                WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                        portfolio,
                        WayangStandardAlignmentPolicyConfig.strict("a2a"),
                        providerDiagnostics);

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("warning");
        assertThat(health.warning()).isTrue();
        assertThat(health.blocked()).isFalse();
        assertThat(health.providerIds()).containsExactly("a2a-provider");
        assertThat(health.providerSummaries()).containsExactly(providerSummary);
        assertThat(health.hasProviders()).isTrue();
        assertThat(health.providerIssues()).containsExactly(issue);
        assertThat(health.providerDiagnostics())
                .satisfies(diagnostics -> {
                    assertThat(diagnostics.healthy()).isFalse();
                    assertThat(diagnostics.providerCount()).isEqualTo(1);
                    assertThat(diagnostics.issueCount()).isEqualTo(1);
                });
        assertThat(health.providerPolicyAssessment())
                .satisfies(assessment -> {
                    assertThat(assessment.ready()).isTrue();
                    assertThat(assessment.issueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.WARN);
                    assertThat(assessment.warning()).isTrue();
                });
        assertThat(health.recommendations())
                .containsExactly("Review standard-alignment provider broken-provider: boom");
        assertThat(health.toMap())
                .containsEntry("providerCount", 1)
                .containsEntry("providerIds", List.of("a2a-provider"))
                .containsEntry("providerIssueCount", 1);
        assertThat(map(health.toMap().get("providerDiagnostics")))
                .containsEntry("healthy", false)
                .containsEntry("providerCount", 1)
                .containsEntry("issueCount", 1);
        assertThat(map(health.toMap().get("providerPolicyAssessment")))
                .containsEntry("ready", true)
                .containsEntry("issueMode", "warn")
                .containsEntry("issueCount", 1);
        assertThat(list(health.toMap().get("providers")))
                .singleElement()
                .satisfies(provider -> assertThat(map(provider))
                        .containsEntry("providerId", "a2a-provider")
                        .containsEntry("priority", 10)
                        .containsEntry("standardIds", List.of("a2a"))
                        .containsEntry("aligned", true));
    }

    @Test
    void providerIssueBlockModeBlocksHealth() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()));

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                portfolio,
                WayangStandardAlignmentPolicyConfig.builder()
                        .mode("strict")
                        .standardId("a2a")
                        .providerIssueMode(WayangStandardAlignmentProviderIssueMode.BLOCK)
                        .build(),
                providerDiagnosticsWithIssue(portfolio));

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.warning()).isFalse();
        assertThat(health.blocked()).isTrue();
        assertThat(health.providerPolicyAssessment().blocked()).isTrue();
        assertThat(health.recommendations())
                .containsExactly("Review standard-alignment provider broken-provider: boom");
    }

    @Test
    void providerIssueIgnoreModeDoesNotWarnHealth() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()));

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                portfolio,
                WayangStandardAlignmentPolicyConfig.builder()
                        .mode("strict")
                        .standardId("a2a")
                        .providerIssueMode(WayangStandardAlignmentProviderIssueMode.IGNORE)
                        .build(),
                providerDiagnosticsWithIssue(portfolio));

        assertThat(health.ready()).isTrue();
        assertThat(health.status()).isEqualTo("ready");
        assertThat(health.warning()).isFalse();
        assertThat(health.blocked()).isFalse();
        assertThat(health.hasProviderIssues()).isTrue();
        assertThat(health.recommendations()).isEmpty();
    }

    @Test
    void requiredProviderPolicyBlocksMissingProviders() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()));

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                portfolio,
                WayangStandardAlignmentPolicyConfig.builder()
                        .mode("strict")
                        .standardId("a2a")
                        .requiredProviderIds("a2a-provider", "a2ui-provider")
                        .minimumProviderCount(2)
                        .build(),
                new WayangStandardAlignmentProviderDiagnostics(
                        List.of("a2a-provider"),
                        List.of(WayangStandardAlignmentProviderSummary.from(
                                "a2a-provider",
                                "example.A2aProvider",
                                10,
                                portfolio)),
                        List.of()));

        assertThat(health.ready()).isFalse();
        assertThat(health.status()).isEqualTo("blocked");
        assertThat(health.blocked()).isTrue();
        assertThat(health.providerPolicyAssessment().missingProviderIds()).containsExactly("a2ui-provider");
        assertThat(health.recommendations())
                .containsExactly(
                        "Register at least 2 standard-alignment provider(s).",
                        "Register required standard-alignment provider: a2ui-provider.");
    }

    @Test
    void diagnosticsOverloadAcceptsNullDiagnosticsAsEmpty() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentPortfolio.fromReportMaps(
                report("a2a", true, 20, 20, 0, List.of(), List.of()));

        WayangStandardAlignmentHealthReport health = WayangStandardAlignmentHealthReport.from(
                portfolio,
                WayangStandardAlignmentPolicy.strict("a2a"),
                WayangStandardRegistryDriftMode.IGNORE,
                (WayangStandardAlignmentProviderDiagnostics) null);

        assertThat(health.ready()).isTrue();
        assertThat(health.hasProviders()).isFalse();
        assertThat(health.hasProviderIssues()).isFalse();
        assertThat(health.providerDiagnostics().healthy()).isTrue();
        assertThat(map(health.toMap().get("providerDiagnostics")))
                .containsEntry("healthy", true)
                .containsEntry("providerCount", 0)
                .containsEntry("issueCount", 0);
    }

    private static WayangStandardAlignmentProviderDiagnostics providerDiagnosticsWithIssue(
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

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangStandardAlignmentMaps.copy((Map<?, ?>) value);
    }

    private static List<?> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<?>) value;
    }
}
