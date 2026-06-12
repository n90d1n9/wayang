package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesLearningPromotionReceiptLedgerFactoryTest {

    @Test
    void createsFileSystemLedgerFromSettings(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("promotion-receipts.jsonl");
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "file-system",
                        "receipt-ledger-path", ledgerPath.toString(),
                        "receipt-ledger-max-records", "3"));

        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerFactory.create(settings, HermesPersistenceResources.empty());

        assertThat(ledger).isInstanceOf(FileSystemHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 3);
    }

    @Test
    void createsObjectStorageLedgerWhenResourceIsAvailable() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "object-storage",
                        "receipt-ledger-object-prefix", "tenant/hermes/promotion-receipts"));

        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedgerFactory.create(
                settings,
                HermesPersistenceResources.of(Optional.of(storage), Optional.empty()));

        assertThat(ledger).isInstanceOf(ObjectStorageHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerPrefix", "tenant/hermes/promotion-receipts/");
    }

    @Test
    void hybridPrefersDatabaseThenFallsBackToObjectStorageOrMemory(@TempDir Path tempDir) {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "hybrid",
                        "receipt-ledger-path", tempDir.resolve("hybrid.jsonl").toString(),
                        "receipt-ledger-database-table", "factory_hybrid_receipts",
                        "receipt-ledger-database-initialize-schema", "off",
                        "receipt-ledger-object-prefix", "factory/hybrid"));

        HermesLearningPromotionReceiptLedger databaseLedger = HermesLearningPromotionReceiptLedgerFactory.create(
                settings,
                HermesPersistenceResources.of(
                        Optional.of(new InMemoryHermesObjectStorageService()),
                        Optional.of(new InMemoryPromotionReceiptDataSource())));
        HermesLearningPromotionReceiptLedger objectStorageLedger = HermesLearningPromotionReceiptLedgerFactory.create(
                settings,
                HermesPersistenceResources.of(Optional.of(new InMemoryHermesObjectStorageService()), Optional.empty()));
        HermesLearningPromotionReceiptLedger memoryLedger = HermesLearningPromotionReceiptLedgerFactory.create(
                settings,
                HermesPersistenceResources.empty());

        assertThat(metadataMap(databaseLedger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "factory_hybrid_receipts");
        assertThat(metadataMap(objectStorageLedger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "factory/hybrid/");
        assertThat(metadataMap(memoryLedger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "in-memory");
    }

    @Test
    void requiresConfiguredDurableResources() {
        HermesLearningPromotionReceiptLedgerSettings objectStorageSettings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "object-storage"));
        HermesLearningPromotionReceiptLedgerSettings databaseSettings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "database",
                        "receipt-ledger-database-initialize-schema", "off"));

        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerFactory.create(
                objectStorageSettings,
                HermesPersistenceResources.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ObjectStorageService");
        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerFactory.create(
                databaseSettings,
                HermesPersistenceResources.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
