package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable status report for one named Hermes learning lifecycle stage.
 */
public record HermesLearningStageReport(
        String stage,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_PENDING = "pending";

    public HermesLearningStageReport {
        stage = HermesDirectiveSupport.clean(stage, "unknown");
        status = normalizeStatus(status);
        reason = HermesText.oneLineOr(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesLearningStageReport completed(String stage) {
        return completed(stage, "", Map.of());
    }

    public static HermesLearningStageReport completed(
            String stage,
            String reason,
            Map<String, Object> metadata) {
        return new HermesLearningStageReport(stage, STATUS_COMPLETED, reason, metadata);
    }

    public static HermesLearningStageReport skipped(String stage, String reason) {
        return new HermesLearningStageReport(stage, STATUS_SKIPPED, reason, Map.of());
    }

    public static HermesLearningStageReport failed(
            String stage,
            String reason,
            Map<String, Object> metadata) {
        return new HermesLearningStageReport(stage, STATUS_FAILED, reason, metadata);
    }

    public static HermesLearningStageReport pending(String stage, String reason) {
        return new HermesLearningStageReport(stage, STATUS_PENDING, reason, Map.of());
    }

    public static Optional<HermesLearningStageReport> fromMetadata(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        return Optional.of(new HermesLearningStageReport(
                text(map.get(HermesLearningMetadataKeys.STAGE)),
                text(map.get(HermesLearningMetadataKeys.STATUS)),
                text(map.get(HermesLearningMetadataKeys.REASON)),
                objectMap(map.get(HermesLearningMetadataKeys.METADATA))));
    }

    public HermesLearningStageReport withMetadata(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(metadata);
        merged.putAll(values);
        return new HermesLearningStageReport(stage, status, reason, merged);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(HermesLearningMetadataKeys.STAGE, stage);
        values.put(HermesLearningMetadataKeys.STATUS, status);
        values.put(HermesLearningMetadataKeys.REASON, reason);
        values.put(HermesLearningMetadataKeys.METADATA, metadata);
        return Map.copyOf(values);
    }

    private static String normalizeStatus(String value) {
        String status = HermesDirectiveSupport.clean(value, STATUS_PENDING);
        return switch (status) {
            case STATUS_COMPLETED, STATUS_SKIPPED, STATUS_FAILED, STATUS_PENDING -> status;
            default -> STATUS_PENDING;
        };
    }

    private static String text(Object value) {
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        return Map.copyOf(values);
    }
}
