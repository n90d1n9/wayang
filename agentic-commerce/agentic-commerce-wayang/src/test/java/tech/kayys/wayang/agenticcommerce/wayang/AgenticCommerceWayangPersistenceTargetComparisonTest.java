package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceTargetComparisonTest {

    @Test
    void unchangedTargetsHaveNoChangeReasons() {
        Map<String, Object> target = Map.of(
                "storageKind",
                "file",
                "targetKind",
                "file",
                "provider",
                "local-file",
                "location",
                "state",
                "durable",
                true);

        AgenticCommerceWayangPersistenceTargetComparison comparison =
                AgenticCommerceWayangPersistenceTargetComparison.between(target, target);

        assertThat(comparison.changed()).isFalse();
        assertThat(comparison.changeReasons()).isEmpty();
        assertThat(comparison.toMap())
                .containsEntry("changed", false)
                .containsEntry("locationChanged", false);
    }

    @Test
    void changedTargetsExplainBackendDrift() {
        AgenticCommerceWayangPersistenceTargetComparison comparison =
                AgenticCommerceWayangPersistenceTargetComparison.between(
                        "previous",
                        Map.of(
                                "storageKind",
                                "file",
                                "targetKind",
                                "file",
                                "provider",
                                "local-file",
                                "location",
                                "state",
                                "durable",
                                true,
                                "cloudStorage",
                                false),
                        "current",
                        Map.of(
                                "storageKind",
                                "object-store",
                                "targetKind",
                                "object-store",
                                "provider",
                                "s3",
                                "location",
                                "wayang-state/prod",
                                "durable",
                                true,
                                "cloudStorage",
                                true));

        assertThat(comparison.changed()).isTrue();
        assertThat(comparison.storageKindChanged()).isTrue();
        assertThat(comparison.targetKindChanged()).isTrue();
        assertThat(comparison.providerChanged()).isTrue();
        assertThat(comparison.locationChanged()).isTrue();
        assertThat(comparison.cloudStorageChanged()).isTrue();
        assertThat(comparison.durabilityChanged()).isFalse();
        assertThat(comparison.changeReasons())
                .containsExactly(
                        "storage_kind_changed",
                        "target_kind_changed",
                        "provider_changed",
                        "location_changed",
                        "cloud_storage_changed");
        assertThat(map(comparison.toMap().get("sourceTarget"))).containsEntry("provider", "local-file");
        assertThat(map(comparison.toMap().get("targetTarget"))).containsEntry("provider", "s3");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
