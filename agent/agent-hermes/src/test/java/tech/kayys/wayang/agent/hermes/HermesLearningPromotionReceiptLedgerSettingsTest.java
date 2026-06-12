package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesLearningPromotionReceiptLedgerSettingsTest {

    @Test
    void derivesEffectiveSettingsFromPersistenceHints() {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                        "receipt-ledger-store", "database",
                        "receipt-ledger-path", "/tmp/hermes/promotion-receipts.jsonl",
                        "receipt-ledger-object-prefix", "cloud/hermes/promotion-receipts",
                        "receipt-ledger-database-table", "tenant_promotion_receipts",
                        "receipt-ledger-database-initialize-schema", "off",
                        "receipt-ledger-max-records", "77"));

        assertThat(settings.store()).isEqualTo(HermesPersistenceStoreKind.DATABASE);
        assertThat(settings.path()).isEqualTo("/tmp/hermes/promotion-receipts.jsonl");
        assertThat(settings.objectPrefix()).isEqualTo("cloud/hermes/promotion-receipts");
        assertThat(settings.jdbcTableName()).isEqualTo("tenant_promotion_receipts");
        assertThat(settings.jdbcInitializeSchema()).isFalse();
        assertThat(settings.maxRecords()).isEqualTo(77);
        assertThat(settings.toMetadata())
                .containsEntry("ledgerStore", "database")
                .containsEntry("ledgerPath", "/tmp/hermes/promotion-receipts.jsonl")
                .containsEntry("ledgerObjectPrefix", "cloud/hermes/promotion-receipts")
                .containsEntry("ledgerJdbcTableName", "tenant_promotion_receipts")
                .containsEntry("ledgerJdbcInitializeSchema", false)
                .containsEntry("maxRecords", 77)
                .containsEntry("configuredBy", "persistenceHints");
    }

    @Test
    void defaultsToNoopWithDurableFileDefaults() {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of());

        assertThat(settings.store()).isEqualTo(HermesPersistenceStoreKind.NOOP);
        assertThat(settings.path()).isEqualTo(HermesLearningPromotionReceiptLedgerSettings.DEFAULT_FILE_SYSTEM_PATH);
        assertThat(settings.objectPrefix()).isEqualTo(ObjectStorageHermesLearningPromotionReceiptLedger.DEFAULT_PREFIX);
        assertThat(settings.jdbcTableName()).isEqualTo(DatabaseHermesLearningPromotionReceiptLedger.DEFAULT_TABLE_NAME);
        assertThat(settings.jdbcInitializeSchema()).isTrue();
        assertThat(settings.maxRecords()).isEqualTo(FileSystemHermesLearningPromotionReceiptLedger.DEFAULT_MAX_RECORDS);
        assertThat(settings.toMetadata())
                .containsEntry("ledgerStore", "noop")
                .containsEntry("durable", false)
                .containsEntry("fileFallback", false)
                .containsEntry("replaySupported", false);
    }

    @Test
    void rejectsInvalidSettings() {
        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                "receipt-ledger-max-records", "0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("learningPromotionReceiptLedgerMaxRecords");

        assertThatThrownBy(() -> HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of(
                "receipt-ledger-database-initialize-schema", "sometimes")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("learningPromotionReceiptLedgerJdbcInitializeSchema");
    }
}
