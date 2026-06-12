package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigSourceSpecsTest {

    @Test
    void buildsCommonLookupAndObjectStorageSpecs() {
        SessionConfigSourceSpec database = SessionConfigSourceSpecs.database("tenant-a", "operators");
        SessionConfigSourceSpec configService = SessionConfigSourceSpecs.configService("a2ui/default");
        SessionConfigSourceSpec s3 = SessionConfigSourceSpecs.s3("wayang-config", "tenant-a/a2ui-session.json");
        SessionConfigSourceSpec rustfs = SessionConfigSourceSpecs.rustfs("wayang-config", "shared/a2ui-session.json");

        assertThat(database.toMap())
                .containsEntry("type", "database")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("profile", "operators");
        assertThat(configService.toMap())
                .containsEntry("type", "config-service")
                .containsEntry("key", "a2ui/default");
        assertThat(s3.toMap())
                .containsEntry("type", "s3")
                .containsEntry("bucket", "wayang-config")
                .containsEntry("key", "tenant-a/a2ui-session.json");
        assertThat(rustfs.toMap())
                .containsEntry("type", "rustfs")
                .containsEntry("bucket", "wayang-config")
                .containsEntry("key", "shared/a2ui-session.json");
    }

    @Test
    void rejectsBlankFactoryInputsBeforeRegistryResolution() {
        assertThatThrownBy(() -> SessionConfigSourceSpecs.database(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId must not be blank");
        assertThatThrownBy(() -> SessionConfigSourceSpecs.configService(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key must not be blank");
        assertThatThrownBy(() -> SessionConfigSourceSpecs.s3("wayang", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key must not be blank");
        assertThatThrownBy(() -> SessionConfigSourceSpecs.objectStorage(" ", "wayang", "a2ui.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type must not be blank");
    }

    @Test
    void resolvesFactorySpecsThroughRegisteredProviders() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database((tenantId, lookupKey, values) -> Optional.empty()))
                .register("s3", SessionConfigObjectStorageProvider.s3((bucket, key, values) -> Optional.of("""
                        {
                          "mode": "run-lifecycle"
                        }
                        """)))
                .build();
        SessionConfigSourceSpec fallback = SessionConfigSourceSpec.firstAvailable(
                SessionConfigSourceSpecs.database("tenant-a"),
                SessionConfigSourceSpecs.s3("wayang-config", "tenant-a/a2ui-session.json"));

        WayangA2uiSessionConfig config = fallback.source(registry).loadOrDefault();

        assertThat(config.actionPolicy().allowedActions()).contains(WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void buildsCustomLookupSpecs() {
        SessionConfigSourceSpec spec = SessionConfigSourceSpecs.lookup("tenant-service", "tenant-a", "operators");

        assertThat(spec.toMap())
                .containsEntry("type", "tenant-service")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("key", "operators");
    }
}
