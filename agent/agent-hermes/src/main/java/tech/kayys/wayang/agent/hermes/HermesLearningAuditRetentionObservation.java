package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Structured outcome from a learning-audit retention observation pass.
 */
public record HermesLearningAuditRetentionObservation(
        String outcome,
        String reason,
        boolean emitted,
        HermesLearningAuditRetentionStatus retentionStatus,
        Optional<HermesRuntimeEvent> event,
        Map<String, Object> metadata) {

    public static final String OUTCOME_EMITTED = "emitted";
    public static final String OUTCOME_SUPPRESSED = "suppressed";
    public static final String OUTCOME_UNAVAILABLE = "unavailable";
    public static final String OUTCOME_FAILED = "failed";

    public HermesLearningAuditRetentionObservation {
        outcome = HermesText.oneLineOr(outcome, emitted ? OUTCOME_EMITTED : OUTCOME_SUPPRESSED);
        reason = HermesText.oneLineOr(reason, "unspecified");
        retentionStatus = retentionStatus == null
                ? HermesLearningAuditRetentionStatus.fromMetadata(Map.of())
                : retentionStatus;
        event = event == null ? Optional.empty() : event;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean requiresAttention() {
        return retentionStatus.requiresAttention();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("outcome", outcome);
        values.put("reason", reason);
        values.put("emitted", emitted);
        values.put("requiresAttention", requiresAttention());
        values.put("retentionState", retentionStatus.status());
        values.put("retentionSeverity", retentionStatus.severity());
        values.put("retentionPriority", retentionStatus.priority());
        values.put("retentionStatus", retentionStatus.toMetadata());
        values.put("event", event.map(HermesRuntimeEvent::toMetadata).orElse(Map.of()));
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    static HermesLearningAuditRetentionObservation emitted(
            HermesLearningAuditRetentionStatus status,
            HermesRuntimeEvent event,
            String reason,
            Map<String, Object> metadata) {
        return new HermesLearningAuditRetentionObservation(
                OUTCOME_EMITTED,
                reason,
                true,
                status,
                Optional.ofNullable(event),
                metadata);
    }

    static HermesLearningAuditRetentionObservation suppressed(
            HermesLearningAuditRetentionStatus status,
            String reason,
            Map<String, Object> metadata) {
        return new HermesLearningAuditRetentionObservation(
                OUTCOME_SUPPRESSED,
                reason,
                false,
                status,
                Optional.empty(),
                metadata);
    }

    static HermesLearningAuditRetentionObservation unavailable(String reason) {
        return new HermesLearningAuditRetentionObservation(
                OUTCOME_UNAVAILABLE,
                reason,
                false,
                HermesLearningAuditRetentionStatus.fromMetadata(Map.of()),
                Optional.empty(),
                Map.of());
    }

    static HermesLearningAuditRetentionObservation failed(Throwable error) {
        return new HermesLearningAuditRetentionObservation(
                OUTCOME_FAILED,
                "observation-error",
                false,
                HermesLearningAuditRetentionStatus.fromMetadata(Map.of()),
                Optional.empty(),
                Map.of(
                        "errorType", error == null ? "" : error.getClass().getName(),
                        "error", error == null ? "" : HermesText.oneLineOr(error.getMessage(), "")));
    }
}
