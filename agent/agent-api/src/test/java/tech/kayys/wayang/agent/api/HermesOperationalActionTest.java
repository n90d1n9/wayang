package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalActionTest {

    @Test
    void normalizesStructuredActionItem() {
        HermesOperationalAction action = new HermesOperationalAction(
                " ",
                "  archive-learning-audit-receipts  ",
                " ",
                -1,
                " ",
                false,
                true,
                List.of("archive-target"),
                "  archive receipts  ",
                Map.of("status", "near-capacity"));

        assertThat(action)
                .extracting(
                        HermesOperationalAction::source,
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::severity,
                        HermesOperationalAction::priority,
                        HermesOperationalAction::riskLevel,
                        HermesOperationalAction::safe,
                        HermesOperationalAction::dryRunSupported,
                        HermesOperationalAction::message)
                .containsExactly(
                        "hermes",
                        "archive-learning-audit-receipts",
                        "info",
                        0,
                        "medium",
                        false,
                        true,
                        "archive receipts");
        assertThat(action.requiredConfig()).containsExactly("archive-target");
        assertThat(action.metadata()).containsEntry("status", "near-capacity");
    }

    @Test
    void mapsKnownRetentionActionsToOperationalActions() {
        List<HermesOperationalAction> actions = HermesOperationalAction.retentionActions(
                "warning",
                2,
                List.of(
                        "monitor-learning-audit-retention",
                        "increase-learning-audit-retention-limit",
                        "increase-learning-audit-retention-limit"));

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0))
                .extracting(
                        HermesOperationalAction::source,
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::severity,
                        HermesOperationalAction::priority,
                        HermesOperationalAction::riskLevel,
                        HermesOperationalAction::safe,
                        HermesOperationalAction::dryRunSupported)
                .containsExactly(
                        "learning-audit-retention",
                        "monitor-learning-audit-retention",
                        "warning",
                        2,
                        "low",
                        true,
                        false);
        assertThat(actions.get(1))
                .extracting(
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::riskLevel,
                        HermesOperationalAction::safe,
                        HermesOperationalAction::dryRunSupported)
                .containsExactly(
                        "increase-learning-audit-retention-limit",
                        "medium",
                        false,
                        true);
        assertThat(actions.get(1).requiredConfig())
                .containsExactly("learning-audit-retention-limit");
    }
}
