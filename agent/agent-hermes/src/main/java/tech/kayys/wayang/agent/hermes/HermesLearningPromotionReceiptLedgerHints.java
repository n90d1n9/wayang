package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical hint names and aliases for learning promotion receipt ledger storage.
 */
final class HermesLearningPromotionReceiptLedgerHints {

    static final String STORE = "learningPromotionReceiptLedgerStore";
    static final String PATH = "learningPromotionReceiptLedgerPath";
    static final String OBJECT_PREFIX = "learningPromotionReceiptLedgerObjectPrefix";
    static final String JDBC_TABLE_NAME = "learningPromotionReceiptLedgerJdbcTableName";
    static final String JDBC_INITIALIZE_SCHEMA = "learningPromotionReceiptLedgerJdbcInitializeSchema";
    static final String MAX_RECORDS = "learningPromotionReceiptLedgerMaxRecords";

    static final List<String> STORE_ALIASES = List.of(
            "learning-promotion-receipt-ledger-store",
            STORE,
            "promotion-receipt-ledger-store",
            "promotionReceiptLedgerStore",
            "receipt-ledger-store",
            "receiptLedgerStore");
    static final List<String> PATH_ALIASES = List.of(
            "learning-promotion-receipt-ledger-path",
            PATH,
            "promotion-receipt-ledger-path",
            "promotionReceiptLedgerPath",
            "receipt-ledger-path",
            "receiptLedgerPath");
    static final List<String> OBJECT_PREFIX_ALIASES = List.of(
            "learning-promotion-receipt-ledger-object-prefix",
            OBJECT_PREFIX,
            "promotion-receipt-ledger-object-prefix",
            "promotionReceiptLedgerObjectPrefix",
            "receipt-ledger-object-prefix",
            "receiptLedgerObjectPrefix");
    static final List<String> JDBC_TABLE_ALIASES = List.of(
            "learning-promotion-receipt-ledger-jdbc-table-name",
            JDBC_TABLE_NAME,
            "learning-promotion-receipt-ledger-database-table",
            "learningPromotionReceiptLedgerDatabaseTable",
            "promotion-receipt-ledger-jdbc-table-name",
            "promotionReceiptLedgerJdbcTableName",
            "promotion-receipt-ledger-database-table",
            "promotionReceiptLedgerDatabaseTable",
            "receipt-ledger-jdbc-table-name",
            "receiptLedgerJdbcTableName",
            "receipt-ledger-database-table",
            "receiptLedgerDatabaseTable");
    static final List<String> JDBC_INITIALIZE_SCHEMA_ALIASES = List.of(
            "learning-promotion-receipt-ledger-jdbc-initialize-schema",
            JDBC_INITIALIZE_SCHEMA,
            "learning-promotion-receipt-ledger-database-initialize-schema",
            "learningPromotionReceiptLedgerDatabaseInitializeSchema",
            "promotion-receipt-ledger-jdbc-initialize-schema",
            "promotionReceiptLedgerJdbcInitializeSchema",
            "promotion-receipt-ledger-database-initialize-schema",
            "promotionReceiptLedgerDatabaseInitializeSchema",
            "receipt-ledger-jdbc-initialize-schema",
            "receiptLedgerJdbcInitializeSchema",
            "receipt-ledger-database-initialize-schema",
            "receiptLedgerDatabaseInitializeSchema");
    static final List<String> MAX_RECORDS_ALIASES = List.of(
            "learning-promotion-receipt-ledger-max-records",
            MAX_RECORDS,
            "promotion-receipt-ledger-max-records",
            "promotionReceiptLedgerMaxRecords",
            "receipt-ledger-max-records",
            "receiptLedgerMaxRecords");

    private HermesLearningPromotionReceiptLedgerHints() {
    }

    static Optional<String> configValue(HermesConfigValues values, List<String> aliases) {
        return values.get(aliases.toArray(String[]::new));
    }

    static Optional<String> hint(Map<String, String> hints, List<String> aliases) {
        if (hints == null || hints.isEmpty()) {
            return Optional.empty();
        }
        List<String> normalizedAliases = aliases.stream()
                .map(HermesConfigValues::normalizeKey)
                .toList();
        return hints.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> normalizedAliases.contains(HermesConfigValues.normalizeKey(entry.getKey())))
                .map(entry -> entry.getValue().trim())
                .findFirst();
    }
}
