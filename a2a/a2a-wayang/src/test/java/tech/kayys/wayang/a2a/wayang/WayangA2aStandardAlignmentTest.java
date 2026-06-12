package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProvider;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aStandardAlignmentTest {

    @Test
    void adaptsNativeA2aReportToSdkPortfolioAndPinnedHealth() {
        WayangStandardAlignmentPortfolio portfolio = WayangA2aStandardAlignment.portfolio();
        WayangStandardAlignmentHealthReport health = WayangA2aStandardAlignment.pinnedHealth();

        assertThat(portfolio.standardIds()).containsExactly("a2a");
        assertThat(portfolio.aligned()).isTrue();
        assertThat(portfolio.registryDrift().driftFree()).isTrue();
        assertThat(health.ready()).isTrue();
        assertThat(health.policyAssessment().requiredStandardIds()).containsExactly("a2a");
        assertThat(health.policyAssessment().requiredVersions()).containsEntry("a2a", A2aProtocol.VERSION);
    }

    @Test
    void configuredHealthPreservesAlignmentGaps() {
        WayangA2aSpecAlignmentReport report = WayangA2aSpecAlignmentReport.from(new A2aHttpRouteCatalog(List.of()));

        WayangStandardAlignmentHealthReport health = WayangA2aStandardAlignment.health(
                report,
                WayangStandardAlignmentPolicyConfig.pinnedRegistry("agent2agent"));

        assertThat(health.ready()).isFalse();
        assertThat(health.policyAssessment().failingStandardIds()).containsExactly("a2a");
        assertThat(health.portfolio().gapStandardIds()).containsExactly("a2a");
    }

    @Test
    void registersStandardAlignmentProviderForServiceDiscovery() {
        assertThat(ServiceLoader.load(WayangStandardAlignmentProvider.class))
                .anySatisfy(provider -> {
                    assertThat(provider).isInstanceOf(WayangA2aStandardAlignmentProvider.class);
                    assertThat(provider.providerId()).isEqualTo("wayang-a2a-standard-alignment");
                    assertThat(provider.portfolio().standardIds()).containsExactly("a2a");
                });
    }
}
