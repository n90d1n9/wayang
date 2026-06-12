package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceWayangPersistenceConfigTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsBuildFileStore() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.defaults();
        AgenticCommerceWayangPersistenceStore store = config.buildStore();

        assertThat(config.storageKind()).isEqualTo(FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.directory()).isEqualTo(AgenticCommerceWayangPersistenceConfig.DEFAULT_DIRECTORY);
        assertThat(config.hybrid()).isFalse();
        assertThat(store).isInstanceOf(FileAgenticCommerceWayangPersistenceStore.class);
        assertThat(store.storageKind()).isEqualTo(FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void fileConfigBindsFromAliases() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "type",
                "local-file",
                "path",
                temporaryDirectory.resolve("file-store").toString()));
        FileAgenticCommerceWayangPersistenceStore store =
                (FileAgenticCommerceWayangPersistenceStore) config.buildStore();

        assertThat(config.storageKind()).isEqualTo(FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.directory()).isEqualTo(temporaryDirectory.resolve("file-store").toString());
        assertThat(store.directory()).isEqualTo(temporaryDirectory.resolve("file-store").toAbsolutePath().normalize());
        assertThat(config.toMap())
                .containsEntry("storageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("directory", temporaryDirectory.resolve("file-store").toString());
    }

    @Test
    void memoryConfigBindsFromAliases() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "type",
                "ephemeral",
                "directory",
                "ignored"));
        AgenticCommerceWayangPersistenceStore store = config.buildStore();

        assertThat(config.storageKind()).isEqualTo(InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.directory()).isBlank();
        assertThat(store).isInstanceOf(InMemoryAgenticCommerceWayangPersistenceStore.class);
        assertThat(store.storageKind()).isEqualTo(InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.toMap())
                .containsEntry("storageKind", InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("directory", "");
    }

    @Test
    void memoryFactoryBuildsInMemoryStore() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.memory();

        assertThat(config.storageKind()).isEqualTo(InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.buildStore()).isInstanceOf(InMemoryAgenticCommerceWayangPersistenceStore.class);
    }

    @Test
    void hybridConfigBuildsPrimaryFallbackStoreFromNestedMaps() {
        Path primaryDirectory = temporaryDirectory.resolve("primary");
        Path fallbackDirectory = temporaryDirectory.resolve("fallback");
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hybrid",
                "primary",
                Map.of(
                        "storageKind",
                        "file",
                        "directory",
                        primaryDirectory.toString()),
                "fallback",
                Map.of(
                        "storageKind",
                        "file",
                        "directory",
                        fallbackDirectory.toString()),
                "mirrorWrites",
                "false"));

        HybridAgenticCommerceWayangPersistenceStore store =
                (HybridAgenticCommerceWayangPersistenceStore) config.buildStore();

        assertThat(config.hybrid()).isTrue();
        assertThat(config.primary().directory()).isEqualTo(primaryDirectory.toString());
        assertThat(config.fallback().directory()).isEqualTo(fallbackDirectory.toString());
        assertThat(config.mirrorWritesToFallback()).isFalse();
        assertThat(map(config.targetDescriptor().toMap()))
                .containsEntry("storageKind", HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("hybrid", true)
                .containsEntry("mirrorWritesToFallback", false)
                .containsEntry("primaryStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("fallbackStorageKind", FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(store.mirrorWritesToFallback()).isFalse();
        assertThat(store.primary()).isInstanceOf(FileAgenticCommerceWayangPersistenceStore.class);
        assertThat(store.fallback()).isInstanceOf(FileAgenticCommerceWayangPersistenceStore.class);
        assertThat(config.toMap()).containsKeys("primary", "fallback");
        assertThat(map(config.toMap().get("target"))).containsEntry("targetKind", "hybrid");
    }

    @Test
    void s3AliasBuildsObjectStoreWhenClientIsInjected() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "s3",
                "endpoint",
                "https://s3.example/",
                "bucket",
                "wayang-state",
                "prefix",
                "prod/agentic-commerce"));

        ObjectStoreAgenticCommerceWayangPersistenceStore store =
                (ObjectStoreAgenticCommerceWayangPersistenceStore) config.buildStore(
                        InMemoryAgenticCommerceObjectStoreClient.create());

        assertThat(config.storageKind()).isEqualTo(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.objectStore()).isTrue();
        assertThat(config.objectStoreConfig().provider()).isEqualTo(AgenticCommerceObjectStoreConfig.PROVIDER_S3);
        assertThat(config.objectStoreConfig().endpoint()).isEqualTo("https://s3.example");
        assertThat(config.objectStoreConfig().bucket()).isEqualTo("wayang-state");
        assertThat(config.objectStoreConfig().keyPrefix()).isEqualTo("prod/agentic-commerce");
        assertThat(store.storageKind()).isEqualTo(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(map(config.toMap().get("objectStore")))
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("bucket", "wayang-state");
        assertThat(map(config.toMap().get("target")))
                .containsEntry("targetKind", "object-store")
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("location", "wayang-state/prod/agentic-commerce")
                .containsEntry("cloudStorage", true);
    }

    @Test
    void objectStoreNoArgBuildRequiresClient() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "rustfs",
                "bucket",
                "wayang-state",
                "directory",
                "prod"));

        assertThat(config.storageKind()).isEqualTo(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.objectStoreConfig().provider()).isEqualTo(AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS);
        assertThatThrownBy(config::buildStore)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an AgenticCommerceObjectStoreClient");
    }

    @Test
    void nestedObjectStoreConfigPreservesProviderAliases() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "object-store",
                "rustfs",
                Map.of(
                        "bucket",
                        "wayang-state",
                        "keyPrefix",
                        "/nested/")));

        assertThat(config.objectStoreConfig().provider()).isEqualTo(AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS);
        assertThat(config.objectStoreConfig().keyPrefix()).isEqualTo("nested");
    }

    @Test
    void unsupportedStorageKindIsExplicitAtBuildTime() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "unsupported-backend",
                "directory",
                "ignored"));

        assertThat(config.storageKind()).isEqualTo("unsupported-backend");
        assertThatThrownBy(config::buildStore)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Agentic Commerce persistence storage kind: unsupported-backend");
    }

    @Test
    void databaseConfigBindsFromAliasesAndRequiresClient() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "postgresql",
                "table",
                "wayang_documents",
                "namespace",
                "/prod/agentic-commerce/"));

        assertThat(config.storageKind()).isEqualTo(DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(config.database()).isTrue();
        assertThat(config.databaseConfig().provider())
                .isEqualTo(AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES);
        assertThat(config.databaseConfig().tableName()).isEqualTo("wayang_documents");
        assertThat(config.databaseConfig().namespace()).isEqualTo("prod/agentic-commerce");
        assertThat(config.toMap())
                .containsEntry("storageKind", DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(map(config.toMap().get("database")))
                .containsEntry("provider", AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES)
                .containsEntry("tableName", "wayang_documents")
                .containsEntry("namespace", "prod/agentic-commerce");
        assertThat(map(config.toMap().get("target")))
                .containsEntry("targetKind", "database")
                .containsEntry("provider", AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES)
                .containsEntry("location", "wayang_documents/prod/agentic-commerce")
                .containsEntry("database", true);
        assertThatThrownBy(config::buildStore)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an AgenticCommerceDatabasePersistenceClient");
    }

    @Test
    void databaseConfigBuildsStoreWithInjectedClient() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "database",
                "tableName",
                "wayang_documents",
                "namespace",
                "embedded"));
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();

        DatabaseAgenticCommerceWayangPersistenceStore store =
                (DatabaseAgenticCommerceWayangPersistenceStore) config.buildStore(client);
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());

        assertThat(store.storageKind()).isEqualTo(DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(client.contains("wayang_documents", "embedded/runtime-config.json")).isTrue();
    }

    @Test
    void hybridRequiresPrimaryAndFallbackConfigs() {
        assertThatThrownBy(() -> AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hybrid",
                "primary",
                Map.of("directory", temporaryDirectory.resolve("primary").toString()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary and fallback");
    }

    @Test
    void configuredServiceBuildsObjectStoreWithInjectedClient() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "object-store",
                "provider",
                "s3-compatible",
                "bucket",
                "wayang-state",
                "keyPrefix",
                "service"));

        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.configured(
                config,
                InMemoryAgenticCommerceObjectStoreClient.create());

        assertThat(service.store()).isInstanceOf(ObjectStoreAgenticCommerceWayangPersistenceStore.class);
        assertThat(service.store().storageKind()).isEqualTo(ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void configuredServiceBuildsDatabaseStoreWithInjectedClient() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "jdbc",
                "table",
                "wayang_documents",
                "namespace",
                "service"));

        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.configured(
                config,
                InMemoryAgenticCommerceDatabasePersistenceClient.create());

        assertThat(service.store()).isInstanceOf(DatabaseAgenticCommerceWayangPersistenceStore.class);
        assertThat(service.store().storageKind()).isEqualTo(DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
    }

    @Test
    void hybridDatabaseConfigRoutesEachStoreThroughResolver() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hybrid",
                "primary",
                Map.of(
                        "storageKind",
                        "postgres",
                        "tableName",
                        "primary_documents",
                        "namespace",
                        "primary"),
                "fallback",
                Map.of(
                        "storageKind",
                        "jdbc",
                        "tableName",
                        "fallback_documents",
                        "namespace",
                        "fallback")));
        InMemoryAgenticCommerceDatabasePersistenceClient primaryClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient fallbackClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceDatabasePersistenceClientRegistry registry =
                AgenticCommerceDatabasePersistenceClientRegistry.builder()
                        .table("primary_documents", primaryClient)
                        .table("fallback_documents", fallbackClient)
                        .build();

        HybridAgenticCommerceWayangPersistenceStore store =
                (HybridAgenticCommerceWayangPersistenceStore) config.buildStore(registry);
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());

        assertThat(store.primary()).isInstanceOf(DatabaseAgenticCommerceWayangPersistenceStore.class);
        assertThat(store.fallback()).isInstanceOf(DatabaseAgenticCommerceWayangPersistenceStore.class);
        assertThat(primaryClient.contains("primary_documents", "primary/runtime-config.json")).isTrue();
        assertThat(fallbackClient.contains("fallback_documents", "fallback/runtime-config.json")).isTrue();
    }

    @Test
    void hybridObjectStoreConfigRoutesEachStoreThroughResolver() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hybrid",
                "primary",
                Map.of(
                        "storageKind",
                        "s3",
                        "bucket",
                        "primary-state",
                        "keyPrefix",
                        "primary"),
                "fallback",
                Map.of(
                        "storageKind",
                        "rustfs",
                        "bucket",
                        "fallback-state",
                        "keyPrefix",
                        "fallback")));
        InMemoryAgenticCommerceObjectStoreClient s3Client = InMemoryAgenticCommerceObjectStoreClient.create();
        InMemoryAgenticCommerceObjectStoreClient rustfsClient = InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceObjectStoreClientRegistry registry = AgenticCommerceObjectStoreClientRegistry.builder()
                .provider("s3", s3Client)
                .provider("rustfs", rustfsClient)
                .build();

        HybridAgenticCommerceWayangPersistenceStore store =
                (HybridAgenticCommerceWayangPersistenceStore) config.buildStore(registry);
        store.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.defaults());

        assertThat(store.primary()).isInstanceOf(ObjectStoreAgenticCommerceWayangPersistenceStore.class);
        assertThat(store.fallback()).isInstanceOf(ObjectStoreAgenticCommerceWayangPersistenceStore.class);
        assertThat(s3Client.contains("primary-state", "primary/runtime-config.json")).isTrue();
        assertThat(rustfsClient.contains("fallback-state", "fallback/runtime-config.json")).isTrue();
    }

    @Test
    void configuredServiceBuildsObjectStoreWithResolver() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "s3",
                "bucket",
                "wayang-state",
                "keyPrefix",
                "service-resolver"));
        AgenticCommerceObjectStoreClientRegistry registry = AgenticCommerceObjectStoreClientRegistry.builder()
                .provider("s3", InMemoryAgenticCommerceObjectStoreClient.create())
                .build();

        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.configured(
                config,
                registry);

        assertThat(service.store()).isInstanceOf(ObjectStoreAgenticCommerceWayangPersistenceStore.class);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
