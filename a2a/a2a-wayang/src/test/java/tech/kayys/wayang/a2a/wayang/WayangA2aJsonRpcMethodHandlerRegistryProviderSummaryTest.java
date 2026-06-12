package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodHandlerRegistryProviderSummaryTest {

    @Test
    void derivesProviderSummaryFromGroupContributions() {
        WayangA2aJsonRpcMethodHandlerRegistryProviderSummary summary =
                WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.from(
                        List.of(WayangA2aJsonRpcMethodRegistryTestFixtures.taskGroupMap()),
                        null,
                        null,
                        null);

        assertThat(summary.providerCount()).isEqualTo(1);
        assertThat(summary.providerIds())
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK);
        assertThat(summary.moduleIds())
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.MODULE_TEST);
        assertThat(summary.capabilityTags()).containsExactly("test", "task");
        assertThat(summary.toMap())
                .containsEntry("providerCount", 1)
                .containsEntry("providerIds", List.of(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK));
    }

    @Test
    void explicitSummaryValuesOverrideGroupDerivation() {
        WayangA2aJsonRpcMethodHandlerRegistryProviderSummary summary =
                WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.from(
                        List.of(WayangA2aJsonRpcMethodRegistryTestFixtures.taskGroupMap()),
                        List.of("db.skill", "file.skill"),
                        List.of("wayang-skills"),
                        List.of("skill", "rag"));

        assertThat(summary.providerCount()).isEqualTo(2);
        assertThat(summary.providerIds()).containsExactly("db.skill", "file.skill");
        assertThat(summary.moduleIds()).containsExactly("wayang-skills");
        assertThat(summary.capabilityTags()).containsExactly("skill", "rag");
    }

    @Test
    void parsesExplicitSummaryValuesFromMap() {
        WayangA2aJsonRpcMethodHandlerRegistryProviderSummary summary =
                WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.fromMap(
                        Map.of(
                                "providerIds", "db.skill file.skill",
                                "moduleIds", List.of("wayang-skills"),
                                "capabilityTags", "skill,rag"),
                        List.of());

        assertThat(summary.providerIds()).containsExactly("db.skill", "file.skill");
        assertThat(summary.moduleIds()).containsExactly("wayang-skills");
        assertThat(summary.capabilityTags()).containsExactly("skill", "rag");
        assertThat(WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.reported(
                Map.of("providerCount", 2))).isTrue();
    }
}
