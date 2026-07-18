package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalAttentionTest {

    @Test
    void normalizesStructuredAttentionItem() {
        HermesOperationalAttention attention = new HermesOperationalAttention(
                " ",
                " ",
                -1,
                "  capacity warning  ",
                "  monitor-learning-audit-retention  ",
                true,
                Map.of("status", "near-capacity"));

        assertThat(attention)
                .extracting(
                        HermesOperationalAttention::source,
                        HermesOperationalAttention::severity,
                        HermesOperationalAttention::priority,
                        HermesOperationalAttention::message,
                        HermesOperationalAttention::action,
                        HermesOperationalAttention::retryable)
                .containsExactly(
                        "hermes",
                        "info",
                        0,
                        "capacity warning",
                        "monitor-learning-audit-retention",
                        true);
        assertThat(attention.metadata()).containsEntry("status", "near-capacity");
    }

    @Test
    void mapsDistinctMessagesToAttentionItems() {
        List<HermesOperationalAttention> items = HermesOperationalAttention.fromMessages(
                "hermes-status",
                "warning",
                2,
                List.of("needs journal", "needs journal", " "));

        assertThat(items).hasSize(1);
        assertThat(items.get(0))
                .extracting(
                        HermesOperationalAttention::source,
                        HermesOperationalAttention::severity,
                        HermesOperationalAttention::priority,
                        HermesOperationalAttention::message)
                .containsExactly("hermes-status", "warning", 2, "needs journal");
    }
}
