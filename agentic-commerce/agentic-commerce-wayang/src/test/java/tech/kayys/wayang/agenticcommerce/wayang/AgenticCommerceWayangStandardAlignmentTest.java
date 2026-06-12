package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceRouteCatalog;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceSpecAlignmentReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProvider;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangStandardAlignmentTest {

    @Test
    void adaptsNativeAgenticCommerceReportToSdkPortfolioAndPinnedHealth() {
        WayangStandardAlignmentPortfolio portfolio = AgenticCommerceWayangStandardAlignment.portfolio();
        WayangStandardAlignmentHealthReport health = AgenticCommerceWayangStandardAlignment.pinnedHealth();

        assertThat(portfolio.standardIds()).containsExactly("agentic-commerce");
        assertThat(portfolio.aligned()).isTrue();
        assertThat(portfolio.registryDrift().driftFree()).isTrue();
        assertThat(health.ready()).isTrue();
        assertThat(health.policyAssessment().requiredStandardIds()).containsExactly("agentic-commerce");
        assertThat(health.policyAssessment().requiredVersions())
                .containsEntry("agentic-commerce", AgenticCommerceProtocol.SPEC_VERSION);
    }

    @Test
    void configuredHealthPreservesAlignmentGaps() {
        AgenticCommerceSpecAlignmentReport report =
                AgenticCommerceSpecAlignmentReport.from(new AgenticCommerceRouteCatalog(List.of()));

        WayangStandardAlignmentHealthReport health = AgenticCommerceWayangStandardAlignment.health(
                report,
                WayangStandardAlignmentPolicyConfig.pinnedRegistry("agenticcommerce"));

        assertThat(health.ready()).isFalse();
        assertThat(health.policyAssessment().failingStandardIds()).containsExactly("agentic-commerce");
        assertThat(health.portfolio().gapStandardIds()).containsExactly("agentic-commerce");
    }

    @Test
    void registersStandardAlignmentProviderForServiceDiscovery() {
        assertThat(ServiceLoader.load(WayangStandardAlignmentProvider.class))
                .anySatisfy(provider -> {
                    assertThat(provider).isInstanceOf(AgenticCommerceWayangStandardAlignmentProvider.class);
                    assertThat(provider.providerId()).isEqualTo("wayang-agentic-commerce-standard-alignment");
                    assertThat(provider.portfolio().standardIds()).containsExactly("agentic-commerce");
                });
    }
}
