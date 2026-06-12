package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed read model for metadata attached to one Hermes learning result.
 */
public record HermesLearningResultMetadata(Map<String, Object> values) {

    public HermesLearningResultMetadata {
        values = HermesMetadata.copy(values);
    }

    public static HermesLearningResultMetadata empty() {
        return new HermesLearningResultMetadata(Map.of());
    }

    public boolean emptyMetadata() {
        return values.isEmpty();
    }

    public Map<String, Object> promotion() {
        return objectMap(HermesLearningMetadataKeys.PROMOTION);
    }

    public Map<String, Object> promotionReceipt() {
        return objectMap(HermesLearningMetadataKeys.PROMOTION_RECEIPT);
    }

    public Map<String, Object> promotionReceiptLedger() {
        return objectMap(HermesLearningMetadataKeys.PROMOTION_RECEIPT_LEDGER);
    }

    public Map<String, Object> skillIndexingReceipt() {
        return objectMap(HermesLearningMetadataKeys.SKILL_INDEXING_RECEIPT);
    }

    public Map<String, Object> lifecycle() {
        return objectMap(HermesLearningMetadataKeys.LIFECYCLE);
    }

    public HermesLearningLifecycleReport lifecycleReport() {
        return HermesLearningLifecycleReport.fromMetadata(lifecycle());
    }

    public Map<String, Object> toMetadata() {
        return values;
    }

    private Map<String, Object> objectMap(String key) {
        Object value = values.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        map.forEach((mapKey, mapValue) -> metadata.put(String.valueOf(mapKey), mapValue));
        return Map.copyOf(metadata);
    }
}
