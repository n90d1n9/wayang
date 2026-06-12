package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceDatabasePersistenceClientRegistryTest {

    @Test
    void resolvesClientsByMostSpecificLocationFirst() {
        InMemoryAgenticCommerceDatabasePersistenceClient providerClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient namespaceClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient exactClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient defaultClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceDatabasePersistenceConfig exactConfig = config("postgres", "wayang_docs", "tenant-a");
        AgenticCommerceDatabasePersistenceClientRegistry registry =
                AgenticCommerceDatabasePersistenceClientRegistry.builder()
                        .defaultClient(defaultClient)
                        .provider("postgres", providerClient)
                        .namespace("tenant-a", namespaceClient)
                        .location(exactConfig, exactClient)
                        .build();

        assertThat(registry.resolve(exactConfig)).isSameAs(exactClient);
        assertThat(registry.resolve(config("postgres", "other_docs", "tenant-a"))).isSameAs(namespaceClient);
        assertThat(registry.resolve(config("postgres", "other_docs", "tenant-b"))).isSameAs(providerClient);
        assertThat(registry.resolve(config("jdbc", "other_docs", "tenant-c"))).isSameAs(defaultClient);
        assertThat(registry.toMap())
                .containsEntry("registeredClientCount", 3)
                .containsEntry("defaultClientConfigured", true);
    }

    @Test
    void resolvesProviderNamespaceBeforeProvider() {
        InMemoryAgenticCommerceDatabasePersistenceClient providerClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient providerNamespaceClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceDatabasePersistenceClientRegistry registry =
                AgenticCommerceDatabasePersistenceClientRegistry.builder()
                        .provider("jdbc", providerClient)
                        .providerNamespace("jdbc", "tenant-state", providerNamespaceClient)
                        .build();

        assertThat(registry.resolve(config("jdbc", "wayang_docs", "tenant-state")))
                .isSameAs(providerNamespaceClient);
        assertThat(registry.resolve(config("jdbc", "wayang_docs", "other-state"))).isSameAs(providerClient);
    }

    @Test
    void resolvesTableNamespaceBeforeTable() {
        InMemoryAgenticCommerceDatabasePersistenceClient tableClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        InMemoryAgenticCommerceDatabasePersistenceClient tableNamespaceClient =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceDatabasePersistenceClientRegistry registry =
                AgenticCommerceDatabasePersistenceClientRegistry.builder()
                        .table("wayang_docs", tableClient)
                        .tableNamespace("wayang_docs", "tenant-a", tableNamespaceClient)
                        .build();

        assertThat(registry.resolve(config("postgres", "wayang_docs", "tenant-a")))
                .isSameAs(tableNamespaceClient);
        assertThat(registry.resolve(config("postgres", "wayang_docs", "tenant-b"))).isSameAs(tableClient);
    }

    @Test
    void throwsWhenNoClientMatches() {
        AgenticCommerceDatabasePersistenceClientRegistry registry =
                AgenticCommerceDatabasePersistenceClientRegistry.builder()
                        .provider("postgres", InMemoryAgenticCommerceDatabasePersistenceClient.create())
                        .build();

        assertThatThrownBy(() -> registry.resolve(config("jdbc", "wayang_docs", "tenant")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Agentic Commerce database persistence client registered");
    }

    private static AgenticCommerceDatabasePersistenceConfig config(
            String provider,
            String tableName,
            String namespace) {
        return AgenticCommerceDatabasePersistenceConfig.fromMap(Map.of(
                "provider",
                provider,
                "tableName",
                tableName,
                "namespace",
                namespace));
    }
}
