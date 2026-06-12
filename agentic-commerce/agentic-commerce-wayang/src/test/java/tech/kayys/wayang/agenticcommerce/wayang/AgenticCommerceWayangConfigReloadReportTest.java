package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangConfigReloadReportTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void unchangedSnapshotsDoNotRecommendReloadWork() {
        AgenticCommerceWayangPersistenceService service = service("unchanged");
        AgenticCommerceWayangConfigSnapshot snapshot = service.snapshot();

        AgenticCommerceWayangConfigReloadReport report = service.reload(snapshot);

        assertThat(report.changed()).isFalse();
        assertThat(report.sourceChanged()).isFalse();
        assertThat(report.runtimeRebuildRecommended()).isFalse();
        assertThat(report.bootstrapRerunRecommended()).isFalse();
        assertThat(report.persistenceTargetChanged()).isFalse();
        assertThat(report.persistenceTargetComparison().changed()).isFalse();
        assertThat(report.changedSections()).isEmpty();
        assertThat(report.changedSources()).isEmpty();
        assertThat(map(report.toMap().get("currentPersistenceTarget")))
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(map(map(report.toMap().get("current")).get("persistenceTarget")))
                .containsEntry("targetKind", "file");
    }

    @Test
    void runtimeConfigChangeRecommendsRuntimeRebuildAndBootstrapRerun() {
        AgenticCommerceWayangPersistenceService service = service("runtime-change");
        AgenticCommerceWayangConfigSnapshot previous = service.snapshot();

        service.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("new-token")
                        .withBaseUrl("https://seller.example/"))
                .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                        .checkoutBasePath("/commerce/acp")
                        .smokePath("/internal/acp/smoke")
                        .bindingReportPath("/internal/acp/binding")
                        .build())
                .build());
        AgenticCommerceWayangConfigReloadReport report = service.reload(previous);

        assertThat(report.changed()).isTrue();
        assertThat(report.runtimeConfigChanged()).isTrue();
        assertThat(report.bootstrapConfigChanged()).isFalse();
        assertThat(report.runtimeRebuildRecommended()).isTrue();
        assertThat(report.bootstrapRerunRecommended()).isTrue();
        assertThat(report.changedSections()).containsExactly("runtimeConfig");
        assertThat(report.changedSources()).containsExactly("runtimeConfigSource");
        assertThat(map(report.toMap().get("current"))).containsEntry("runtimeConfigSource", "persisted");
    }

    @Test
    void bootstrapConfigChangeOnlyRecommendsBootstrapRerun() {
        AgenticCommerceWayangPersistenceService service = service("bootstrap-change");
        service.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());
        AgenticCommerceWayangConfigSnapshot previous = service.snapshot();

        service.saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT))
                .includeRuntimeSkills(false)
                .build());
        AgenticCommerceWayangConfigReloadReport report = service.reload(previous);

        assertThat(report.changed()).isTrue();
        assertThat(report.runtimeConfigChanged()).isFalse();
        assertThat(report.bootstrapConfigChanged()).isTrue();
        assertThat(report.runtimeRebuildRecommended()).isFalse();
        assertThat(report.bootstrapRerunRecommended()).isTrue();
        assertThat(report.changedSections()).containsExactly("bootstrapConfig");
        assertThat(report.changedSources()).containsExactly("bootstrapConfigSource");
    }

    @Test
    void persistedDefaultConfigChangesSourceButNotEffectiveConfig() {
        AgenticCommerceWayangPersistenceService service = service("source-change");
        AgenticCommerceWayangConfigSnapshot previous = service.snapshot();

        service.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());
        service.saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig.defaults());
        AgenticCommerceWayangConfigReloadReport report = service.reload(previous);

        assertThat(report.changed()).isFalse();
        assertThat(report.sourceChanged()).isTrue();
        assertThat(report.runtimeRebuildRecommended()).isFalse();
        assertThat(report.bootstrapRerunRecommended()).isFalse();
        assertThat(report.changedSections()).isEmpty();
        assertThat(report.changedSources())
                .containsExactly("runtimeConfigSource", "bootstrapConfigSource");
    }

    @Test
    void persistenceTargetChangeIsReportedSeparatelyFromConfigChanges() {
        AgenticCommerceWayangConfigSnapshot previous = snapshot(Map.of(
                "storageKind",
                "file",
                "directory",
                "local-state"));
        AgenticCommerceWayangConfigSnapshot current = snapshot(Map.of(
                "storageKind",
                "object-store",
                "objectStore",
                Map.of(
                        "provider",
                        "s3",
                        "bucket",
                        "wayang-state",
                        "keyPrefix",
                        "prod")));

        AgenticCommerceWayangConfigReloadReport report =
                AgenticCommerceWayangConfigReloadReport.from(previous, current);

        assertThat(report.changed()).isFalse();
        assertThat(report.sourceChanged()).isFalse();
        assertThat(report.persistenceTargetChanged()).isTrue();
        assertThat(report.persistenceTargetComparison().changeReasons())
                .contains(
                        "storage_kind_changed",
                        "target_kind_changed",
                        "provider_changed",
                        "location_changed",
                        "cloud_storage_changed");
        assertThat(report.runtimeRebuildRecommended()).isFalse();
        assertThat(report.bootstrapRerunRecommended()).isFalse();
        assertThat(report.changedSections()).isEmpty();
        assertThat(report.changedSources()).isEmpty();
        assertThat(map(report.toMap().get("previousPersistenceTarget")))
                .containsEntry("targetKind", "file")
                .containsEntry("location", "local-state");
        assertThat(map(report.toMap().get("currentPersistenceTarget")))
                .containsEntry("targetKind", "object-store")
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("location", "wayang-state/prod")
                .containsEntry("cloudStorage", true);
        assertThat(map(report.toMap().get("persistenceTargetComparison")))
                .containsEntry("changed", true)
                .containsEntry("cloudStorageChanged", true);
    }

    private AgenticCommerceWayangPersistenceService service(String name) {
        return AgenticCommerceWayangPersistenceService.of(
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve(name)));
    }

    private static AgenticCommerceWayangConfigSnapshot snapshot(Map<String, Object> storeStatus) {
        return new AgenticCommerceWayangConfigSnapshot(
                AgenticCommerceWayangRuntimeConfig.defaults(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                true,
                true,
                storeStatus);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
