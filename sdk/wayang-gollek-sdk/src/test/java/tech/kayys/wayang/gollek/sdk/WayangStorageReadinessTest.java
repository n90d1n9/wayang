package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStorageReadinessTest {

    @Test
    void treatsMemoryStorageAsReadyButNonPersistent() {
        WayangReadinessReport report = WayangStorageReadiness.assess(WayangStorageConfig.memory());

        assertThat(report.readinessId()).isEqualTo(WayangStorageReadiness.READINESS_ID);
        assertThat(report.ready()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.probes()).hasSize(5);
        assertThat(report.attributes())
                .containsEntry("backend", "memory")
                .containsEntry("persistent", false)
                .containsEntry("fileFallbackEnabled", false);
        assertThat(probe(report, "storage.retention"))
                .containsEntry("required", false)
                .containsEntry("passed", true);
    }

    @Test
    void flagsIncompleteRustfsObjectStorageForProduction() {
        WayangStorageConfig config = WayangStorageConfig.fromMap(Map.of(
                "backend", "rustfs",
                "bucket", "wayang-runs"));

        WayangReadinessReport report = WayangStorageReadiness.assess(config);

        assertThat(report.ready()).isFalse();
        assertThat(report.exitCode()).isEqualTo(WayangReadinessReports.EXIT_FAILURE);
        assertThat(report.issues())
                .extracting(issue -> issue.get("code"))
                .containsExactly("storage_object_endpoint_missing", "storage_file_fallback_missing");
        assertThat(report.probes())
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "storage.object_storage")
                        .containsEntry("passed", false))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "storage.file_fallback")
                        .containsEntry("passed", false));
    }

    @Test
    void acceptsHybridDatabaseObjectStorageAndFileFallback() {
        WayangStorageConfig config = WayangStorageConfig.hybrid(
                "jdbc:postgresql://localhost:5432/wayang",
                WayangObjectStorageConfig.rustfs("http://localhost:9000", "wayang-runs", "runs"),
                "var/wayang/runs.properties");

        WayangReadinessReport report = WayangStorageReadiness.assess(config);

        assertThat(report.ready()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.attributes())
                .containsEntry("backend", "hybrid")
                .containsEntry("persistent", true)
                .containsEntry("fileFallbackEnabled", true)
                .containsEntry("effectiveFilePath", "var/wayang/runs.properties");
        assertThat(probe(report, "storage.database")).containsEntry("passed", true);
        assertThat(probe(report, "storage.object_storage")).containsEntry("passed", true);
        assertThat(probe(report, "storage.file_fallback")).containsEntry("passed", true);
        assertThat(probe(report, "storage.retention"))
                .containsEntry("required", true)
                .containsEntry("passed", true);
    }

    @Test
    void reportsConfiguredRunStoreRetentionPolicy() {
        WayangStorageConfig config = WayangStorageConfig.file("runs.properties")
                .withRetentionPolicy(AgentRunStoreRetentionPolicy.of(7, 3));

        WayangReadinessReport report = WayangStorageReadiness.assess(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> retentionProbeAttributes =
                (Map<String, Object>) probe(report, "storage.retention").get("attributes");
        @SuppressWarnings("unchecked")
        Map<String, Object> retentionAttributes = (Map<String, Object>) report.attributes().get("retention");

        assertThat(retentionProbeAttributes)
                .containsEntry("maxRuns", 7)
                .containsEntry("maxEventsPerRun", 3)
                .containsEntry("runsBounded", true)
                .containsEntry("eventsPerRunBounded", true)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
        assertThat(retentionAttributes)
                .containsEntry("maxRuns", 7)
                .containsEntry("maxEventsPerRun", 3);
    }

    @Test
    void storageConfigAndReadinessReportsRedactDatabaseUrlSecrets() {
        WayangStorageConfig config = WayangStorageConfig.database(
                "jdbc:postgresql://ops:super-secret@localhost:5432/wayang?password=top-secret&token=api-secret",
                "var/wayang/runs.properties");

        WayangReadinessReport report = WayangStorageReadiness.assess(config);

        assertThat(config.databaseUrl()).contains("top-secret");
        assertThat(String.valueOf(config.toMap()))
                .contains("password=<redacted>")
                .contains("token=<redacted>")
                .contains("ops:<redacted>@localhost")
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
        assertThat(String.valueOf(report.toMap()))
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
    }

    @Test
    void localSdkReportsConfiguredStorageReadiness(@TempDir Path tempDir) {
        Path fallback = tempDir.resolve("runs.properties");
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withStorage(WayangStorageConfig.objectStorage(
                        WayangObjectStorageConfig.s3("https://s3.example.test", "wayang-runs", "ap-southeast-1", ""),
                        fallback.toString())));

        WayangReadinessReport report = sdk.storageReadiness();

        assertThat(report.ready()).isTrue();
        assertThat(report.attributes())
                .containsEntry("backend", "object_storage")
                .containsEntry("effectiveFilePath", fallback.toString());
    }

    private static Map<String, Object> probe(WayangReadinessReport report, String probeName) {
        return report.probes().stream()
                .filter(probe -> probeName.equals(probe.get("probe")))
                .findFirst()
                .orElseThrow();
    }
}
