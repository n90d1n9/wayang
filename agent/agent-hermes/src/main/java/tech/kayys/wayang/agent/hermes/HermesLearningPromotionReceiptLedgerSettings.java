package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Effective storage settings for the learning promotion receipt ledger.
 */
record HermesLearningPromotionReceiptLedgerSettings(
        HermesPersistenceStoreKind store,
        String path,
        String objectPrefix,
        String jdbcTableName,
        boolean jdbcInitializeSchema,
        int maxRecords) {

    static final String DEFAULT_FILE_SYSTEM_PATH =
            "var/hermes/learning-promotion-receipts.jsonl";

    static HermesLearningPromotionReceiptLedgerSettings from(HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        return fromHints(effectiveConfig.persistenceHints());
    }

    static HermesLearningPromotionReceiptLedgerSettings fromHints(Map<String, String> hints) {
        return new HermesLearningPromotionReceiptLedgerSettings(
                store(hints),
                path(hints),
                objectPrefix(hints),
                jdbcTableName(hints),
                jdbcInitializeSchema(hints),
                maxRecords(hints));
    }

    Map<String, Object> toMetadata() {
        HermesPersistenceStoreShape storeShape = HermesPersistenceStoreShape.of(store);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerStore", store.configValue());
        values.put("ledgerPath", path);
        values.put("ledgerObjectPrefix", objectPrefix);
        values.put("ledgerJdbcTableName", jdbcTableName);
        values.put("ledgerJdbcInitializeSchema", jdbcInitializeSchema);
        values.put("maxRecords", maxRecords);
        storeShape.putStorageMetadata(values);
        values.put("replaySupported", storeShape.replaySupported());
        values.put("configuredBy", "persistenceHints");
        return Map.copyOf(values);
    }

    private static HermesPersistenceStoreKind store(Map<String, String> hints) {
        return HermesPersistenceStoreKind.repairStore(
                HermesLearningPromotionReceiptLedgerHints.hint(
                                hints,
                                HermesLearningPromotionReceiptLedgerHints.STORE_ALIASES)
                        .orElse(HermesPersistenceStoreKind.NOOP.configValue()),
                "learningPromotionReceiptLedgerStore");
    }

    private static String path(Map<String, String> hints) {
        return HermesText.trimOr(
                HermesLearningPromotionReceiptLedgerHints.hint(
                                hints,
                                HermesLearningPromotionReceiptLedgerHints.PATH_ALIASES)
                        .orElse(DEFAULT_FILE_SYSTEM_PATH),
                DEFAULT_FILE_SYSTEM_PATH);
    }

    private static String objectPrefix(Map<String, String> hints) {
        return HermesText.trimOr(
                HermesLearningPromotionReceiptLedgerHints.hint(
                                hints,
                                HermesLearningPromotionReceiptLedgerHints.OBJECT_PREFIX_ALIASES)
                        .orElse(ObjectStorageHermesLearningPromotionReceiptLedger.DEFAULT_PREFIX),
                ObjectStorageHermesLearningPromotionReceiptLedger.DEFAULT_PREFIX);
    }

    private static String jdbcTableName(Map<String, String> hints) {
        return DatabaseHermesLearningPromotionReceiptLedger.normalizeTableName(
                HermesLearningPromotionReceiptLedgerHints.hint(
                                hints,
                                HermesLearningPromotionReceiptLedgerHints.JDBC_TABLE_ALIASES)
                        .orElse(DatabaseHermesLearningPromotionReceiptLedger.DEFAULT_TABLE_NAME));
    }

    private static boolean jdbcInitializeSchema(Map<String, String> hints) {
        return HermesLearningPromotionReceiptLedgerHints.hint(
                        hints,
                        HermesLearningPromotionReceiptLedgerHints.JDBC_INITIALIZE_SCHEMA_ALIASES)
                .map(HermesLearningPromotionReceiptLedgerSettings::booleanValue)
                .orElse(true);
    }

    private static int maxRecords(Map<String, String> hints) {
        Optional<String> value = HermesLearningPromotionReceiptLedgerHints.hint(
                hints,
                HermesLearningPromotionReceiptLedgerHints.MAX_RECORDS_ALIASES);
        if (value.isEmpty()) {
            return FileSystemHermesLearningPromotionReceiptLedger.DEFAULT_MAX_RECORDS;
        }
        try {
            int parsed = Integer.parseInt(value.get().trim());
            if (parsed < 1) {
                throw new IllegalArgumentException("learningPromotionReceiptLedgerMaxRecords must be positive");
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("learningPromotionReceiptLedgerMaxRecords must be an integer", error);
        }
    }

    private static boolean booleanValue(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new IllegalArgumentException(
                    "learningPromotionReceiptLedgerJdbcInitializeSchema must be boolean");
        };
    }
}
