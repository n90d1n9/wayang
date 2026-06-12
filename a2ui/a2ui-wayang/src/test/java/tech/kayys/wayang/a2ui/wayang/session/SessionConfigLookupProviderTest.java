package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigLookupProviderTest {

    @Test
    void loadsTenantScopedDatabaseConfigWithDefaultProfile() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database((tenantId, lookupKey, values) -> {
                    assertThat(tenantId).isEqualTo("tenant-a");
                    assertThat(lookupKey).isEqualTo("default");
                    assertThat(values).containsEntry("tenantId", "tenant-a");
                    return Optional.of("""
                            {
                              "mode": "run-lifecycle",
                              "allowedRunIds": ["run-database"]
                            }
                            """);
                }))
                .build();

        SessionConfigSource source = registry.source(Map.of(
                "type", "database",
                "tenantId", "tenant-a"));
        WayangA2uiSessionConfig config = source.loadOrDefault();

        assertThat(source.description()).isEqualTo("database:tenant-a/default");
        assertThat(config.actionPolicy().allowedActions()).contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(config.actionPolicy().allowedRunIds()).containsExactly("run-database");
    }

    @Test
    void supportsConfigServiceLookupAliases() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("config-service", SessionConfigLookupProvider.configService((tenantId, lookupKey, values) -> {
                    assertThat(tenantId).isEqualTo("");
                    assertThat(lookupKey).isEqualTo("a2ui/default");
                    return Optional.of("""
                            {
                              "mode": "read-only"
                            }
                            """);
                }))
                .build();

        WayangA2uiSessionConfig config = registry.source(Map.of(
                "type", "config-service",
                "configKey", "a2ui/default"))
                .loadOrDefault();

        assertThat(config).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void usesExplicitTenantScopedProfileWhenProvided() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database((tenantId, lookupKey, values) -> {
                    assertThat(tenantId).isEqualTo("tenant-a");
                    assertThat(lookupKey).isEqualTo("operators");
                    return Optional.of("""
                            {
                              "mode": "read-only"
                            }
                            """);
                }))
                .build();

        SessionConfigSource source = registry.source(Map.of(
                "type", "database",
                "tenant", "tenant-a",
                "profile", "operators"));

        assertThat(source.description()).isEqualTo("database:tenant-a/operators");
        assertThat(source.loadOrDefault()).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void rejectsMissingRequiredLookupFieldsBeforeReading() {
        SessionConfigSourceRegistry registry = SessionConfigSourceRegistry.standardBuilder()
                .register("database", SessionConfigLookupProvider.database(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .register("config-service", SessionConfigLookupProvider.configService(
                        (tenantId, lookupKey, values) -> Optional.empty()))
                .build();

        assertThatThrownBy(() -> registry.source(Map.of("type", "database")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider spec for database")
                .hasMessageContaining("database source requires tenantId");
        assertThatThrownBy(() -> registry.source(Map.of("type", "config-service")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider spec for config-service")
                .hasMessageContaining("config-service source requires key");
    }

    @Test
    void publishesLookupCapabilityMetadata() {
        SessionConfigSourceCapability database = SessionConfigLookupProvider
                .database((tenantId, lookupKey, values) -> Optional.empty())
                .capability("database");
        SessionConfigSourceCapability configService = SessionConfigLookupProvider
                .configService((tenantId, lookupKey, values) -> Optional.empty())
                .capability("config-service");

        assertThat(database.requiresTenant()).isTrue();
        assertThat(database.requiresLookupKey()).isFalse();
        assertThat(database.lookupKeyScoped()).isTrue();
        assertThat(configService.requiresTenant()).isFalse();
        assertThat(configService.requiresLookupKey()).isTrue();
        assertThat(configService.diagnosticSafeFields()).contains("key", "configKey", "profile");
    }
}
