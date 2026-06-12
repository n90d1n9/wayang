package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesConfigSectionTest {

    @Test
    void persistenceHintSectionMergesConfiguredHintsWithDefaults() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.persistence-hints.definitions", "database",
                "wayang.agent.hermes.persistence-hints.artifacts", "s3"), HermesAgentModeConfigs.PROPERTY_PREFIX);
        HermesAgentModeConfig.Builder builder = HermesAgentModeConfig.builder();

        HermesPersistenceHintsConfigSection.withPrefix(HermesAgentModeConfigs.PROPERTY_PREFIX)
                .apply(values, builder);

        assertThat(builder.build().persistenceHints())
                .containsEntry("fallback", "file-system")
                .containsEntry("definitions", "database")
                .containsEntry("artifacts", "s3");
    }

    @Test
    void persistenceHintSectionMapsLearningPromotionReceiptLedgerAliases() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.learning-promotion-receipt-ledger-store", "database",
                "wayang.agent.hermes.learning-promotion-receipt-ledger-path",
                "/var/lib/wayang/hermes/promotion-receipts.jsonl",
                "wayang.agent.hermes.learning-promotion-receipt-ledger-object-prefix",
                "cloud/hermes/promotion-receipts",
                "wayang.agent.hermes.learning-promotion-receipt-ledger-jdbc-table-name",
                "wayang_promotion_receipts",
                "wayang.agent.hermes.learning-promotion-receipt-ledger-jdbc-initialize-schema", "false",
                "wayang.agent.hermes.learning-promotion-receipt-ledger-max-records", "128"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);
        HermesAgentModeConfig.Builder builder = HermesAgentModeConfig.builder();

        HermesPersistenceHintsConfigSection.withPrefix(HermesAgentModeConfigs.PROPERTY_PREFIX)
                .apply(values, builder);

        assertThat(builder.build().persistenceHints())
                .containsEntry("fallback", "file-system")
                .containsEntry("learningPromotionReceiptLedgerStore", "database")
                .containsEntry(
                        "learningPromotionReceiptLedgerPath",
                        "/var/lib/wayang/hermes/promotion-receipts.jsonl")
                .containsEntry(
                        "learningPromotionReceiptLedgerObjectPrefix",
                        "cloud/hermes/promotion-receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcTableName", "wayang_promotion_receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcInitializeSchema", "false")
                .containsEntry("learningPromotionReceiptLedgerMaxRecords", "128");
    }

    @Test
    void receiptLedgerAliasesOverrideGenericPersistenceHints() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.persistence-hints.learningPromotionReceiptLedgerStore", "file-system",
                "wayang.agent.hermes.promotion-receipt-ledger-store", "database"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);
        HermesAgentModeConfig.Builder builder = HermesAgentModeConfig.builder();

        HermesPersistenceHintsConfigSection.withPrefix(HermesAgentModeConfigs.PROPERTY_PREFIX)
                .apply(values, builder);

        HermesAgentModeConfig config = builder.build();

        assertThat(config.persistenceHints())
                .containsEntry("learningPromotionReceiptLedgerStore", "database");
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "database");
    }
}
