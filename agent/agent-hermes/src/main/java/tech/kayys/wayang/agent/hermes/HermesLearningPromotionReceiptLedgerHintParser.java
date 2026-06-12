package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses first-class config aliases into learning promotion receipt ledger hints.
 */
final class HermesLearningPromotionReceiptLedgerHintParser {

    private HermesLearningPromotionReceiptLedgerHintParser() {
    }

    static Map<String, String> parse(HermesConfigValues values) {
        Map<String, String> hints = new LinkedHashMap<>();
        put(hints, HermesLearningPromotionReceiptLedgerHints.STORE,
                HermesLearningPromotionReceiptLedgerHints.STORE_ALIASES, values);
        put(hints, HermesLearningPromotionReceiptLedgerHints.PATH,
                HermesLearningPromotionReceiptLedgerHints.PATH_ALIASES, values);
        put(hints, HermesLearningPromotionReceiptLedgerHints.OBJECT_PREFIX,
                HermesLearningPromotionReceiptLedgerHints.OBJECT_PREFIX_ALIASES, values);
        put(hints, HermesLearningPromotionReceiptLedgerHints.JDBC_TABLE_NAME,
                HermesLearningPromotionReceiptLedgerHints.JDBC_TABLE_ALIASES, values);
        put(hints, HermesLearningPromotionReceiptLedgerHints.JDBC_INITIALIZE_SCHEMA,
                HermesLearningPromotionReceiptLedgerHints.JDBC_INITIALIZE_SCHEMA_ALIASES, values);
        put(hints, HermesLearningPromotionReceiptLedgerHints.MAX_RECORDS,
                HermesLearningPromotionReceiptLedgerHints.MAX_RECORDS_ALIASES, values);
        return hints;
    }

    private static void put(
            Map<String, String> hints,
            String hint,
            List<String> aliases,
            HermesConfigValues values) {
        HermesLearningPromotionReceiptLedgerHints.configValue(values, aliases)
                .ifPresent(configured -> hints.put(hint, configured));
    }
}
