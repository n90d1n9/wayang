package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigSourceRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsBuiltInSourcesFromDeclarativeSpecs() {
        WayangA2uiSessionConfig inline = SessionConfigSources.fromSpec(Map.of(
                "type", "inline",
                "json", """
                        {
                          "mode": "read-only"
                        }
                        """))
                .load()
                .orElseThrow();
        WayangA2uiSessionConfig classpath = SessionConfigSources.fromSpecJson("""
                {
                  "type": "classpath",
                  "resource": "a2ui/session-config-readonly.json"
                }
                """)
                .load()
                .orElseThrow();

        assertThat(inline).isEqualTo(WayangA2uiSessionConfig.readOnly());
        assertThat(classpath).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void supportsDatabaseFirstFileFallbackWithoutDatabaseDependency() throws Exception {
        Path fallbackFile = tempDir.resolve("fallback-session.json");
        Files.writeString(fallbackFile, """
                {
                  "mode": "run-lifecycle",
                  "allowedRunIds": ["run-file"]
                }
                """, StandardCharsets.UTF_8);
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .build();

        SessionConfigSource source = registry.source(Map.of(
                "type", "fallback",
                "sources", List.of(
                        Map.of("type", "database", "tenantId", "tenant-a"),
                        Map.of("type", "file", "path", fallbackFile.toString()))));
        WayangA2uiSessionConfig config = source.load().orElseThrow();

        assertThat(source.description()).contains("database:tenant-a", "file:");
        assertThat(config.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_CANCEL);
        assertThat(config.actionPolicy().allowedRunIds()).containsExactly("run-file");
    }

    @Test
    void registersObjectStorageProvidersByName() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("s3", SessionConfigObjectStorageProvider.s3((bucket, key, values) -> Optional.of("""
                        {
                          "mode": "run-lifecycle"
                        }
                        """)))
                .register("rustfs", SessionConfigObjectStorageProvider.rustfs((bucket, key, values) -> Optional.of("""
                        {
                          "mode": "read-only"
                        }
                        """)))
                .build();

        assertThat(registry.providerNames()).contains("s3", "rustfs");
        assertThat(registry.source(Map.of("type", "s3", "bucket", "wayang", "key", "a2ui/session.json"))
                .loadOrDefault()
                .actionPolicy()
                .allowedActions())
                .contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(registry.source(Map.of("type", "rustfs", "bucket", "wayang", "key", "a2ui/session.json"))
                .loadOrDefault()
                .actionPolicy()
                .allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
    }

    @Test
    void exposesSourceCapabilityMetadataForBuiltInAndRegisteredProviders() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .register("s3", SessionConfigObjectStorageProvider.s3(
                        (bucket, key, values) -> Optional.empty()))
                .register("custom", values -> SessionConfigSources.inlineJson("custom", "{}"))
                .build();

        assertThat(registry.providerCapabilities().keySet())
                .contains("inline", "file", "classpath", "database", "s3", "custom");
        assertThat(registry.sourceCapabilities().keySet())
                .contains("fallback", "first-available", "chain");
        assertThat(registry.capability("database"))
                .isPresent()
                .get()
                .matches(SessionConfigSourceCapability::requiresTenant)
                .matches(SessionConfigSourceCapability::lookupKeyScoped);
        assertThat(registry.capability("s3"))
                .isPresent()
                .get()
                .matches(SessionConfigSourceCapability::requiresBucketKey)
                .matches(SessionConfigSourceCapability::objectStorage);
        assertThat(registry.capability("fallback"))
                .isPresent()
                .get()
                .matches(SessionConfigSourceCapability::supportsFallback);
        assertThat(registry.capability("custom"))
                .isPresent()
                .get()
                .extracting(SessionConfigSourceCapability::category)
                .isEqualTo(SessionConfigSourceCapability.CATEGORY_CUSTOM);
    }

    @Test
    void enforcesConfiguredSourcePolicyBeforeProviderResolution() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .register("s3", SessionConfigObjectStorageProvider.s3(
                        (bucket, key, values) -> Optional.of("""
                                {
                                  "mode": "read-only"
                                }
                                """)))
                .policy(SessionConfigSourcePolicy.allowOnly("fallback", "database", "s3", "file"))
                .build();

        SessionConfigSource source = registry.source(Map.of(
                "type",
                "fallback",
                "sources",
                List.of(
                        Map.of("type", "database", "tenantId", "tenant-a"),
                        Map.of("type", "s3", "bucket", "wayang", "key", "tenant/a2ui.json"))));

        assertThat(registry.sourcePolicy().allows("inline")).isFalse();
        assertThat(source.loadOrDefault()).isEqualTo(WayangA2uiSessionConfig.readOnly());
        assertThatThrownBy(() -> registry.source(Map.of(
                "type",
                "fallback",
                "sources",
                List.of(
                        Map.of("type", "database", "tenantId", "tenant-a"),
                        Map.of("type", "inline", "json", "{}")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policy rejected")
                .hasMessageContaining("source.sources[1] source type inline is not allowed");
    }

    @Test
    void withPolicyReturnsRestrictedRegistryWithoutDroppingProviders() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("s3", SessionConfigObjectStorageProvider.s3(
                        (bucket, key, values) -> Optional.empty()))
                .build()
                .withPolicy(SessionConfigSourcePolicy.deny("file"));

        assertThat(registry.providerNames()).contains("s3");
        assertThatThrownBy(() -> registry.source(Map.of(
                "type", "file",
                "path", tempDir.resolve("session.json").toString())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source type file is not allowed");
    }

    @Test
    void letsProvidersRejectStorageSpecificSpecsBeforeLoading() {
        SessionConfigSourceProvider provider = new SessionConfigSourceProvider() {
            @Override
            public SessionConfigSource source(Map<String, Object> values) {
                return SessionConfigSources.inlineJson("s3:" + values.get("bucket") + "/" + values.get("key"), """
                        {
                          "mode": "run-lifecycle"
                        }
                        """);
            }

            @Override
            public List<String> validationErrors(Map<String, Object> values) {
                if (String.valueOf(values.getOrDefault("key", "")).isBlank()) {
                    return List.of("s3 source requires key");
                }
                return List.of();
            }
        };
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("s3", provider)
                .build();

        assertThatThrownBy(() -> registry.source(Map.of("type", "s3", "bucket", "wayang")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider spec for s3")
                .hasMessageContaining("s3 source requires key");
    }

    @Test
    void rejectsUnknownProviderTypesClearly() {
        assertThatThrownBy(() -> SessionConfigSourceRegistry.standard().source(Map.of("type", "database")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database");
    }

    @Test
    void rejectsMalformedRawSourceSpecsBeforeProviderResolution() {
        assertThatThrownBy(() -> SessionConfigSourceRegistry.standard().source(Map.of("type", "inline")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inline source requires json");
        assertThatThrownBy(() -> SessionConfigSourceRegistry.standard().source(Map.of(
                "type",
                "fallback",
                "sources",
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one nested source");
    }
}
