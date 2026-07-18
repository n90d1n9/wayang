package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.client.SdkCounts;
import tech.kayys.wayang.contract.WayangContractKey;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SdkCountsTest {

    @Test
    void copiesCountsAsImmutableOrderedMap() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("running", 2);
        counts.put("completed", 1);

        Map<String, Integer> copy = SdkCounts.copy(counts);

        assertThat(copy)
                .containsExactly(
                        Map.entry("running", 2),
                        Map.entry("completed", 1));
        assertThatThrownBy(() -> copy.put("failed", 1))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(SdkCounts.copy(null)).isEmpty();
    }

    @Test
    void copiesPositiveTextKeyCountsWithNormalization() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(" Runs ", 2);
        counts.put(" ", 4);
        counts.put("Run Specs", 0);
        counts.put("Workbench", -1);
        counts.put("Platform", null);

        Map<String, Integer> copy = SdkCounts.copyPositiveTextKeys(counts);

        assertThat(copy)
                .containsExactly(Map.entry("Runs", 2));
    }

    @Test
    void copiesPositiveKeyCounts() {
        WayangContractKey key = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);
        Map<WayangContractKey, Integer> counts = new LinkedHashMap<>();
        counts.put(key, 2);
        counts.put(null, 3);

        Map<WayangContractKey, Integer> copy = SdkCounts.copyPositiveKeys(counts);

        assertThat(copy)
                .containsExactly(Map.entry(key, 2));
    }
}
