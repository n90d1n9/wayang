package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentProviderDiagnosticsTest {

    @Test
    void normalizesProvidersAndIssuesIntoUnifiedDiagnostics() {
        WayangStandardAlignmentProviderSummary provider = provider("a2a-provider", "a2a");
        WayangStandardAlignmentProviderIssue issue = issue("broken-provider", "boom");

        WayangStandardAlignmentProviderDiagnostics diagnostics =
                new WayangStandardAlignmentProviderDiagnostics(
                        List.of(),
                        Arrays.asList(null, provider),
                        Arrays.asList(null, issue));

        assertThat(diagnostics.providerIds()).containsExactly("a2a-provider");
        assertThat(diagnostics.providers()).containsExactly(provider);
        assertThat(diagnostics.issues()).containsExactly(issue);
        assertThat(diagnostics.providerCount()).isEqualTo(1);
        assertThat(diagnostics.issueCount()).isEqualTo(1);
        assertThat(diagnostics.hasProviders()).isTrue();
        assertThat(diagnostics.hasIssues()).isTrue();
        assertThat(diagnostics.healthy()).isFalse();
        assertThat(diagnostics.providerMaps())
                .singleElement()
                .satisfies(payload -> assertThat(payload)
                        .containsEntry("providerId", "a2a-provider")
                        .containsEntry("standardIds", List.of("a2a")));
        assertThat(diagnostics.issueMaps())
                .singleElement()
                .satisfies(payload -> assertThat(payload)
                        .containsEntry("providerId", "broken-provider")
                        .containsEntry("message", "boom"));
        assertThat(diagnostics.toMap())
                .containsEntry("healthy", false)
                .containsEntry("providerCount", 1)
                .containsEntry("providerIds", List.of("a2a-provider"))
                .containsEntry("issueCount", 1);
    }

    @Test
    void diagnosticsAreHealthyWhenNoIssuesArePresent() {
        WayangStandardAlignmentProviderDiagnostics diagnostics =
                new WayangStandardAlignmentProviderDiagnostics(
                        List.of("a2a-provider"),
                        List.of(provider("a2a-provider", "a2a")),
                        List.of());

        assertThat(diagnostics.healthy()).isTrue();
        assertThat(diagnostics.hasProviders()).isTrue();
        assertThat(diagnostics.hasIssues()).isFalse();
    }

    @Test
    void emptyDiagnosticsHaveNoProvidersOrIssues() {
        WayangStandardAlignmentProviderDiagnostics diagnostics = WayangStandardAlignmentProviderDiagnostics.empty();

        assertThat(diagnostics.providerIds()).isEmpty();
        assertThat(diagnostics.providers()).isEmpty();
        assertThat(diagnostics.issues()).isEmpty();
        assertThat(diagnostics.providerCount()).isZero();
        assertThat(diagnostics.issueCount()).isZero();
        assertThat(diagnostics.healthy()).isTrue();
    }

    private static WayangStandardAlignmentProviderSummary provider(String providerId, String standardId) {
        return WayangStandardAlignmentProviderSummary.from(
                providerId,
                "example.Provider",
                10,
                WayangStandardAlignmentPortfolio.fromReportMaps(report(standardId)));
    }

    private static WayangStandardAlignmentProviderIssue issue(String providerId, String message) {
        return new WayangStandardAlignmentProviderIssue(
                providerId,
                "example.Provider",
                message);
    }

    private static Map<String, Object> report(String standardId) {
        WayangStandardDefinition definition = WayangStandardRegistry.find(standardId).orElseThrow();
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
