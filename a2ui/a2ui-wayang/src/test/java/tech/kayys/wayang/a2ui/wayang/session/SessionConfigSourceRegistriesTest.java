package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigSourceRegistriesTest {

    @Test
    void buildsReadyDatabaseRegistry() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistries.database(
                (tenantId, lookupKey, values) -> Optional.of("""
                        {
                          "mode": "read-only"
                        }
                        """));

        WayangA2uiSessionConfig config = registry.source(SessionConfigSourceSpecs.database("tenant-a"))
                .loadOrDefault();

        assertThat(registry.providerNames()).contains("database");
        assertThat(config).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void buildsReadyObjectStorageRegistry() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistries.objectStorage(
                (bucket, key, values) -> Optional.of("""
                        {
                          "mode": "run-lifecycle"
                        }
                        """),
                (bucket, key, values) -> Optional.of("""
                        {
                          "mode": "read-only"
                        }
                        """));

        assertThat(registry.providerNames()).contains("s3", "rustfs");
        assertThat(registry.source(SessionConfigSourceSpecs.s3("wayang", "a2ui/session.json"))
                .loadOrDefault()
                .actionPolicy()
                .allowedActions())
                .contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(registry.source(SessionConfigSourceSpecs.rustfs("wayang", "a2ui/session.json"))
                .loadOrDefault())
                .isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void composesCommonAndCustomProvidersThroughBuilder() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistries
                .registerS3(
                        SessionConfigSourceRegistries.registerDatabase(
                                SessionConfigSourceRegistry.standardBuilder(),
                                (tenantId, lookupKey, values) -> Optional.empty()),
                        (bucket, key, values) -> Optional.empty())
                .register("memory", values -> SessionConfigSources.inlineJson("memory", """
                        {
                          "mode": "run-lifecycle"
                        }
                        """))
                .build();

        WayangA2uiSessionConfig config = registry.source(Map.of("type", "memory"))
                .loadOrDefault();

        assertThat(registry.providerNames()).contains("database", "s3", "memory");
        assertThat(config.actionPolicy().allowedActions()).contains(WayangA2uiActions.RUN_CANCEL);
    }
}
