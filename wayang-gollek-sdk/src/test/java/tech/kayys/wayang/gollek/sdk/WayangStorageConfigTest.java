package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStorageConfigTest {

    @Test
    void normalizesBackendAliasesForProductionStorage() {
        assertThat(WayangStorageBackend.from("filesystem")).isEqualTo(WayangStorageBackend.FILE);
        assertThat(WayangStorageBackend.from("postgres")).isEqualTo(WayangStorageBackend.DATABASE);
        assertThat(WayangStorageBackend.from("s3")).isEqualTo(WayangStorageBackend.OBJECT_STORAGE);
        assertThat(WayangStorageBackend.from("rustfs")).isEqualTo(WayangStorageBackend.OBJECT_STORAGE);
        assertThat(WayangStorageBackend.from("primary-with-fallback")).isEqualTo(WayangStorageBackend.HYBRID);
    }

    @Test
    void parsesObjectStorageWithFileFallback() {
        WayangStorageConfig config = WayangStorageConfig.fromMap(Map.of(
                "backend", "rustfs",
                "endpoint", " http://localhost:9000 ",
                "bucket", "wayang-skills",
                "keyPrefix", "tenants/default",
                "fallbackFilePath", "var/wayang/skills.json"));

        assertThat(config.backend()).isEqualTo(WayangStorageBackend.OBJECT_STORAGE);
        assertThat(config.objectStorage().provider()).isEqualTo("rustfs");
        assertThat(config.objectStorage().endpoint()).isEqualTo("http://localhost:9000");
        assertThat(config.objectStorage().bucket()).isEqualTo("wayang-skills");
        assertThat(config.objectStorage().pathStyleAccess()).isTrue();
        assertThat(config.fallbackToFile()).isTrue();
        assertThat(config.effectiveFilePath()).isEqualTo("var/wayang/skills.json");
        assertThat(config.toMap())
                .containsEntry("backend", "object_storage")
                .containsEntry("fallbackFilePath", "var/wayang/skills.json")
                .containsEntry("effectiveFilePath", "var/wayang/skills.json");
    }

    @Test
    void objectStorageConfigRedactsInlineCredentialPayloadsButKeepsReferenceNames() {
        WayangObjectStorageConfig reference = WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "s3",
                "bucket", "wayang",
                "keyPrefix", "profiles/default.properties",
                "credentialsRef", "prod-readiness-profiles"));
        WayangObjectStorageConfig inlineCredentials = WayangObjectStorageConfig.fromMap(Map.of(
                "provider", "s3",
                "bucket", "wayang",
                "keyPrefix", "profiles/default.properties",
                "credentials", "accessKeyId=inline-access secretAccessKey=inline-secret apiKey:inline-token"));

        assertThat(reference.toMap())
                .containsEntry("credentialsRef", "prod-readiness-profiles");
        assertThat(inlineCredentials.credentialsRef())
                .contains("inline-access")
                .contains("inline-secret")
                .contains("inline-token");
        assertThat(String.valueOf(inlineCredentials.toMap()))
                .contains("accessKeyId=<redacted>")
                .contains("secretAccessKey=<redacted>")
                .contains("apiKey:<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token");
        assertThat(String.valueOf(WayangStorageConfig.objectStorage(
                        inlineCredentials,
                        "var/wayang/runs.properties")
                .toMap()))
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token");
    }

    @Test
    void keepsLegacyRunStorePathAsFileStorageAlias() {
        WayangGollekSdkConfig config = new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.LOCAL,
                "",
                "",
                "tenant-a",
                "model-a",
                "runs.properties");

        assertThat(config.runStorePath()).isEqualTo("runs.properties");
        assertThat(config.storage().backend()).isEqualTo(WayangStorageBackend.FILE);
        assertThat(config.storage().effectiveFilePath()).isEqualTo("runs.properties");
    }

    @Test
    void configuredRunStoreUsesFileFallbackForCloudStorage(@TempDir Path tempDir) {
        Path fallback = tempDir.resolve("runs.properties");
        WayangGollekSdkConfig config = WayangGollekSdkConfig.local()
                .withStorage(WayangStorageConfig.objectStorage(
                        WayangObjectStorageConfig.s3("https://s3.example.test", "wayang", "ap-southeast-1", "runs"),
                        fallback.toString()));

        AgentRunStore store = AgentRunStore.configured(config);

        assertThat(store).isInstanceOf(FileAgentRunStore.class);
        assertThat(((FileAgentRunStore) store).path()).isEqualTo(fallback);
    }

    @Test
    void parsesRunStoreRetentionPolicyFromStorageMap() {
        WayangStorageConfig config = WayangStorageConfig.fromMap(Map.of(
                "backend", "file",
                "path", "runs.properties",
                "retention", Map.of(
                        "maxRuns", 7,
                        "maxEventsPerRun", 3)));
        Map<String, Object> values = config.toMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> retention = (Map<String, Object>) values.get("retention");

        assertThat(config.retentionPolicy()).isEqualTo(AgentRunStoreRetentionPolicy.of(7, 3));
        assertThat(retention)
                .containsEntry("mode", "bounded")
                .containsEntry("maxRuns", 7)
                .containsEntry("maxEventsPerRun", 3)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
    }

    @Test
    void parsesUnlimitedRunStoreRetentionPolicyFromStorageMap() {
        WayangStorageConfig config = WayangStorageConfig.fromMap(Map.of(
                "backend", "file",
                "path", "runs.properties",
                "retention", Map.of("mode", "unlimited")));

        assertThat(config.retentionPolicy()).isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(config.toMap().get("retention").toString())
                .contains("mode=unlimited")
                .contains("maxRuns=0")
                .contains("maxEventsPerRun=0")
                .contains("bounded=false")
                .contains("unlimited=true");
    }

    @Test
    void parsesRunStoreBackupRetentionPolicyFromStorageMap() {
        WayangStorageConfig config = WayangStorageConfig.fromMap(Map.of(
                "backend", "file",
                "path", "runs.properties",
                "backupRetention", Map.of("maxBackups", 2)));
        @SuppressWarnings("unchecked")
        Map<String, Object> backupRetention = (Map<String, Object>) config.toMap().get("backupRetention");

        assertThat(config.backupRetentionPolicy()).isEqualTo(AgentRunStoreBackupRetentionPolicy.of(2));
        assertThat(backupRetention)
                .containsEntry("mode", "bounded")
                .containsEntry("maxBackups", 2)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
    }

    @Test
    void configuredRunStoreUsesConfiguredRetentionPolicy(@TempDir Path tempDir) {
        Path storePath = tempDir.resolve("runs.properties");
        WayangGollekSdkConfig config = WayangGollekSdkConfig.local()
                .withStorage(WayangStorageConfig.fromMap(Map.of(
                        "backend", "file",
                        "path", storePath.toString(),
                        "retention", Map.of("maxRuns", 1, "maxEventsPerRun", 1))));
        AgentRunStore store = AgentRunStore.configured(config);

        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-storage-retention-1", "strategy-a"),
                true,
                "first",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-storage-retention-2", "strategy-a"),
                true,
                "second",
                Map.of()));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(reader.history().runs())
                .extracting(status -> status.handle().runId())
                .containsExactly("run-storage-retention-2");
        assertThat(reader.timeline("run-storage-retention-2").events())
                .singleElement()
                .satisfies(event -> assertThat(event.message()).isEqualTo("second"));
    }
}
