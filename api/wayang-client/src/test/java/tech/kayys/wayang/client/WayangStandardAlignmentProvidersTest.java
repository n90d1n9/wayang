package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProvider;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderDiscovery;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderSummary;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentProvidersTest {

    @Test
    void composesProviderPortfoliosInPriorityOrder() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentProviders.portfolio(List.of(
                provider("agentic-commerce-provider", 20, "agentic-commerce"),
                provider("a2a-provider", 10, "a2a")));

        assertThat(portfolio.standardIds()).containsExactly("a2a", "agentic-commerce");
    }

    @Test
    void portfolioReturnsEmptyWhenNoProvidersAreRegistered() {
        WayangStandardAlignmentPortfolio portfolio = WayangStandardAlignmentProviders.portfolio(List.of());

        assertThat(portfolio.standardCount()).isZero();
        assertThat(portfolio.aligned()).isTrue();
    }

    @Test
    void discoveryIncludesProviderSummariesInCompositionOrder() {
        WayangStandardAlignmentProviderDiscovery discovery = WayangStandardAlignmentProviders.discover(List.of(
                provider("agentic-commerce-provider", 20, "agentic-commerce"),
                provider("a2a-provider", 10, "a2a")));

        assertThat(discovery.providerIds()).containsExactly("a2a-provider", "agentic-commerce-provider");
        assertThat(discovery.providerSummaries())
                .extracting(WayangStandardAlignmentProviderSummary::providerId)
                .containsExactly("a2a-provider", "agentic-commerce-provider");
        assertThat(discovery.providerDiagnostics())
                .satisfies(diagnostics -> {
                    assertThat(diagnostics.healthy()).isTrue();
                    assertThat(diagnostics.providerCount()).isEqualTo(2);
                    assertThat(diagnostics.issueCount()).isZero();
                    assertThat(diagnostics.providerIds())
                            .containsExactly("a2a-provider", "agentic-commerce-provider");
                });
        assertThat(discovery.providerSummaries().get(0))
                .satisfies(summary -> {
                    assertThat(summary.providerClass()).isNotBlank();
                    assertThat(summary.priority()).isEqualTo(10);
                    assertThat(summary.standardIds()).containsExactly("a2a");
                    assertThat(summary.standardCount()).isEqualTo(1);
                    assertThat(summary.aligned()).isTrue();
                    assertThat(summary.hasGaps()).isFalse();
                });
        assertThat(discovery.toMap())
                .containsEntry("providerCount", 2)
                .containsKey("providers");
    }

    @Test
    void isolatesBrokenProviderPortfoliosAsDiscoveryIssues() {
        WayangStandardAlignmentProviderDiscovery discovery = WayangStandardAlignmentProviders.discover(List.of(
                provider("a2a-provider", 10, "a2a"),
                brokenProvider("broken-provider", 20)));

        assertThat(discovery.healthy()).isFalse();
        assertThat(discovery.providerIds()).containsExactly("a2a-provider");
        assertThat(discovery.providerSummaries())
                .extracting(WayangStandardAlignmentProviderSummary::providerId)
                .containsExactly("a2a-provider");
        assertThat(discovery.providerDiagnostics())
                .satisfies(diagnostics -> {
                    assertThat(diagnostics.healthy()).isFalse();
                    assertThat(diagnostics.providerCount()).isEqualTo(1);
                    assertThat(diagnostics.issueCount()).isEqualTo(1);
                });
        assertThat(discovery.portfolio().standardIds()).containsExactly("a2a");
        assertThat(discovery.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.providerId()).isEqualTo("broken-provider");
                    assertThat(issue.message()).contains("boom");
                    assertThat(issue.recommendation()).contains("broken-provider");
                });
    }

    private static WayangStandardAlignmentProvider provider(String providerId, int priority, String standardId) {
        return new WayangStandardAlignmentProvider() {
            @Override
            public String providerId() {
                return providerId;
            }

            @Override
            public WayangStandardAlignmentPortfolio portfolio() {
                return WayangStandardAlignmentPortfolio.fromReportMaps(report(standardId));
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    private static WayangStandardAlignmentProvider brokenProvider(String providerId, int priority) {
        return new WayangStandardAlignmentProvider() {
            @Override
            public String providerId() {
                return providerId;
            }

            @Override
            public WayangStandardAlignmentPortfolio portfolio() {
                throw new IllegalStateException("boom");
            }

            @Override
            public int priority() {
                return priority;
            }
        };
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
