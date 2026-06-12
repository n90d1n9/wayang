package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAgenticCommerceWayangPersistenceStoreTest {

    @Test
    void roundTripsThroughPersistenceContractHarness() {
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        DatabaseAgenticCommerceWayangPersistenceStore store = DatabaseAgenticCommerceWayangPersistenceStore.configured(
                new AgenticCommerceDatabasePersistenceConfig(
                        "jdbc",
                        "wayang_documents",
                        "contract",
                        Map.of("profile", "test")),
                client);

        AgenticCommerceWayangPersistenceContractReport report =
                AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(client.contains("wayang_documents", "contract/runtime-config.json")).isTrue();
        assertThat(client.contains("wayang_documents", "contract/bootstrap-config.json")).isTrue();
        assertThat(store.toMap())
                .containsEntry("storageKind", DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND)
                .containsEntry("runtimeConfigAvailable", true)
                .containsEntry("bootstrapConfigAvailable", true)
                .containsEntry("bootstrapReportAvailable", true)
                .containsEntry("manifestAvailable", true);
        assertThat(map(store.toMap().get("database")))
                .containsEntry("provider", AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC)
                .containsEntry("tableName", "wayang_documents")
                .containsEntry("namespace", "contract");
        assertThat(map(store.toMap().get("target")))
                .containsEntry("targetKind", "database")
                .containsEntry("provider", AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC)
                .containsEntry("location", "wayang_documents/contract")
                .containsEntry("database", true)
                .containsEntry("durable", true);
    }

    @Test
    void healthReportClassifiesDatabaseStoreAsCompleteDurableStorage() {
        DatabaseAgenticCommerceWayangPersistenceStore store = DatabaseAgenticCommerceWayangPersistenceStore.configured(
                AgenticCommerceDatabasePersistenceConfig.defaults(),
                InMemoryAgenticCommerceDatabasePersistenceClient.create());
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        AgenticCommerceWayangPersistenceHealthReport report =
                AgenticCommerceWayangPersistenceHealthReport.from(store);

        assertThat(report.ready()).isTrue();
        assertThat(report.complete()).isTrue();
        assertThat(report.capabilities().durable()).isTrue();
        assertThat(report.capabilities().storageKind())
                .isEqualTo(DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND);
        assertThat(map(report.storeStatus().get("target")))
                .containsEntry("targetKind", "database")
                .containsEntry("database", true);
        assertThat(report.warningFindings()).isEmpty();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
