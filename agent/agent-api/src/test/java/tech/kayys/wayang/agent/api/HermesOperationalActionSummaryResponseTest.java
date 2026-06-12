package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalActionSummaryResponseTest {

    @Test
    void summarizesOperationalActionRiskAndConfiguration() {
        HermesOperationalActionSummaryResponse response =
                HermesOperationalActionSummaryResponse.from(HermesOperationalAction.retentionActions(
                        "warning",
                        2,
                        List.of(
                                "monitor-learning-audit-retention",
                                "archive-learning-audit-receipts")));

        assertThat(response)
                .extracting(
                        HermesOperationalActionSummaryResponse::totalActions,
                        HermesOperationalActionSummaryResponse::safeActions,
                        HermesOperationalActionSummaryResponse::unsafeActions,
                        HermesOperationalActionSummaryResponse::dryRunSupportedActions,
                        HermesOperationalActionSummaryResponse::requiresOperatorApproval,
                        HermesOperationalActionSummaryResponse::requiresConfiguration)
                .containsExactly(2, 1, 1, 1, true, true);
        assertThat(response.requiredConfig()).containsExactly("learning-audit-archive-target");
        assertThat(response.riskLevelCounts())
                .containsEntry("low", 1L)
                .containsEntry("medium", 1L);
    }

    @Test
    void handlesEmptyActions() {
        HermesOperationalActionSummaryResponse response = HermesOperationalActionSummaryResponse.empty();

        assertThat(response.totalActions()).isZero();
        assertThat(response.requiresOperatorApproval()).isFalse();
        assertThat(response.requiresConfiguration()).isFalse();
        assertThat(response.requiredConfig()).isEmpty();
        assertThat(response.riskLevelCounts()).isEmpty();
    }
}
