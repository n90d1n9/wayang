package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigObjectStorageProviderTest {

    @Test
    void loadsSessionConfigThroughBucketKeyReader() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("s3", SessionConfigObjectStorageProvider.s3((bucket, key, values) -> Optional.of("""
                        {
                          "mode": "run-lifecycle",
                          "allowedRunIds": ["run-s3"]
                        }
                        """)))
                .build();

        SessionConfigSource source = registry.source(Map.of(
                "type", "s3",
                "bucket", "wayang-config",
                "key", "tenants/tenant-a/a2ui-session.json"));
        WayangA2uiSessionConfig config = source.loadOrDefault();

        assertThat(source.description()).isEqualTo("s3:wayang-config/tenants/tenant-a/a2ui-session.json");
        assertThat(config.actionPolicy().allowedActions()).contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(config.actionPolicy().allowedRunIds()).containsExactly("run-s3");
    }

    @Test
    void supportsRustfsAliasesWithoutRustfsDependency() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("rustfs", SessionConfigObjectStorageProvider.rustfs((bucket, key, values) -> Optional.of("""
                        {
                          "mode": "read-only"
                        }
                        """)))
                .build();

        WayangA2uiSessionConfig config = registry.source(Map.of(
                "type", "rustfs",
                "container", "wayang-config",
                "objectKey", "shared/a2ui-session.json"))
                .loadOrDefault();

        assertThat(config).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void rejectsMissingBucketOrKeyBeforeReading() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("s3", SessionConfigObjectStorageProvider.s3((bucket, key, values) -> Optional.empty()))
                .build();

        assertThatThrownBy(() -> registry.source(Map.of("type", "s3", "bucket", "wayang-config")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider spec for s3")
                .hasMessageContaining("s3 source requires key");
        assertThatThrownBy(() -> registry.source(Map.of("type", "s3", "key", "tenant/a2ui-session.json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("s3 source requires bucket");
    }

    @Test
    void publishesObjectStorageCapabilityMetadata() {
        SessionConfigSourceCapability capability = SessionConfigObjectStorageProvider
                .s3((bucket, key, values) -> Optional.empty())
                .capability("s3");

        assertThat(capability.objectStorage()).isTrue();
        assertThat(capability.requiresBucketKey()).isTrue();
        assertThat(capability.requiredFields()).containsExactly("bucket", "key");
        assertThat(capability.diagnosticSafeFields()).contains("bucket", "container", "key", "objectKey");
    }
}
