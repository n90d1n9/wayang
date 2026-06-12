package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesLearningPromotionReceiptLedgerResolverTest {

    @Test
    void defaultsToNoopLedger() {
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(null);

        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "noop")
                .containsEntry("recordCount", 0)
                .containsEntry("replaySupported", false);
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(null))
                .containsEntry("ledgerStore", "noop")
                .containsEntry("ledgerPath", HermesLearningPromotionReceiptLedgerResolver.DEFAULT_FILE_SYSTEM_PATH)
                .containsEntry("durable", false)
                .containsEntry("replaySupported", false)
                .containsEntry("configuredBy", "persistenceHints");
    }

    @Test
    void resolvesFileSystemLedgerFromPersistenceHints(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("promotion-receipts.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "learning-promotion-receipt-ledger-store", "file-system",
                        "learning-promotion-receipt-ledger-path", ledgerPath.toString(),
                        "learning-promotion-receipt-ledger-max-records", "2"))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(config);

        HermesLearningPromotionReceipt receipt = ledger.record(receipt("key-file", "skill-file"));
        HermesLearningPromotionReceiptLedger replayLedger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(config);

        assertThat(ledger).isInstanceOf(FileSystemHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 2);
        assertThat(Files.exists(ledgerPath)).isTrue();
        assertThat(replayLedger.find("key-file")).contains(receipt);
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 2)
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true)
                .containsEntry("replaySupported", true);
    }

    @Test
    void resolvesObjectStorageLedgerFromPersistenceHints() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "learning-promotion-receipt-ledger-store", "object-storage",
                        "learning-promotion-receipt-ledger-object-prefix", "tenant-a/hermes/promotion-receipts",
                        "learning-promotion-receipt-ledger-max-records", "2"))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(config, java.util.Optional.of(storage));

        ledger.record(receipt("key-object-1", "skill-object-1"));
        HermesLearningPromotionReceipt second = ledger.record(receipt("key-object-2", "skill-object-2"));
        ledger.record(receipt("key-object-3", "skill-object-3"));

        assertThat(ledger).isInstanceOf(ObjectStorageHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "tenant-a/hermes/promotion-receipts/")
                .containsEntry("recordCount", 2)
                .containsEntry("maxRecords", 2);
        assertThat(storage.objects).hasSize(2);
        assertThat(ledger.find("key-object-1")).isEmpty();
        assertThat(ledger.find("key-object-2")).contains(second);
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "object-storage")
                .containsEntry("ledgerObjectPrefix", "tenant-a/hermes/promotion-receipts")
                .containsEntry("objectStorageCapable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void objectStorageLedgerRequiresObjectStorageService() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of("receipt-ledger-store", "object-storage"))
                .build();

        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ObjectStorageService");
    }

    @Test
    void resolvesDatabaseLedgerFromPersistenceHints() {
        InMemoryPromotionReceiptDataSource dataSource = new InMemoryPromotionReceiptDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "learning-promotion-receipt-ledger-store", "database",
                        "learning-promotion-receipt-ledger-jdbc-table-name", "hermes_receipt_ledger",
                        "learning-promotion-receipt-ledger-jdbc-initialize-schema", "false",
                        "learning-promotion-receipt-ledger-max-records", "2"))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));

        ledger.record(receipt("key-database-1", "skill-database-1"));
        HermesLearningPromotionReceipt second = ledger.record(receipt("key-database-2", "skill-database-2"));
        ledger.record(receipt("key-database-3", "skill-database-3"));

        assertThat(ledger).isInstanceOf(DatabaseHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "hermes_receipt_ledger")
                .containsEntry("recordCount", 2)
                .containsEntry("maxRecords", 2);
        assertThat(ledger.find("key-database-1")).isEmpty();
        assertThat(ledger.find("key-database-2")).contains(second);
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "database")
                .containsEntry("ledgerJdbcTableName", "hermes_receipt_ledger")
                .containsEntry("ledgerJdbcInitializeSchema", false)
                .containsEntry("databaseCapable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void resolvesDatabaseLedgerFromLegacyDatabaseAliases() {
        InMemoryPromotionReceiptDataSource dataSource = new InMemoryPromotionReceiptDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "receipt-ledger-store", "database",
                        "receipt-ledger-database-table", "legacy_receipt_ledger",
                        "receipt-ledger-database-initialize-schema", "off"))
                .build();

        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));

        assertThat(ledger).isInstanceOf(DatabaseHermesLearningPromotionReceiptLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerTable", "legacy_receipt_ledger");
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerJdbcTableName", "legacy_receipt_ledger")
                .containsEntry("ledgerJdbcInitializeSchema", false);
    }

    @Test
    void databaseLedgerRequiresDataSource() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of("receipt-ledger-store", "database"))
                .build();

        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    @Test
    void resolvesHybridLedgerWithFileFallbackFromPersistenceHints(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("hybrid-promotion-receipts.jsonl");
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "promotionReceiptLedgerStore", "hybrid",
                        "promotionReceiptLedgerPath", ledgerPath.toString(),
                        "promotionReceiptLedgerObjectPrefix", "tenant-a/hermes/hybrid-promotion-receipts",
                        "promotionReceiptLedgerMaxRecords", "4"))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(config, java.util.Optional.of(storage));

        ledger.record(receipt("key-hybrid", "skill-hybrid"));

        assertThat(ledger).isInstanceOf(HybridHermesLearningPromotionReceiptLedger.class);
        assertThat(metadataMap(ledger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "tenant-a/hermes/hybrid-promotion-receipts/");
        assertThat(metadataMap(ledger.toMetadata(), "fallbackLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString());
        assertThat(storage.objects).hasSize(1);
        assertThat(Files.exists(ledgerPath)).isTrue();
        assertThat(HermesLearningPromotionReceiptLedgerResolver.resolve(config).find("key-hybrid"))
                .isPresent();
    }

    @Test
    void resolvesHybridLedgerWithDatabasePrimaryWhenDataSourceIsAvailable(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("hybrid-database-promotion-receipts.jsonl");
        InMemoryPromotionReceiptDataSource dataSource = new InMemoryPromotionReceiptDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "promotionReceiptLedgerStore", "hybrid",
                        "promotionReceiptLedgerPath", ledgerPath.toString(),
                        "promotionReceiptLedgerJdbcTableName", "hermes_hybrid_receipts",
                        "promotionReceiptLedgerMaxRecords", "4"))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));

        ledger.record(receipt("key-hybrid-database", "skill-hybrid-database"));

        assertThat(metadataMap(ledger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "hermes_hybrid_receipts");
        assertThat(metadataMap(ledger.toMetadata(), "fallbackLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString());
        assertThat(Files.exists(ledgerPath)).isTrue();
        assertThat(HermesLearningPromotionReceiptLedgerResolver.resolve(config).find("key-hybrid-database"))
                .isPresent();
    }

    private static HermesLearningPromotionReceipt receipt(String idempotencyKey, String skillId) {
        return new HermesLearningPromotionReceipt(
                "promotion-" + skillId,
                idempotencyKey,
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                skillId,
                true,
                "persisted",
                "resolver",
                "definitions=resolver,artifacts=file-system",
                Map.of("adapterId", "resolver"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
