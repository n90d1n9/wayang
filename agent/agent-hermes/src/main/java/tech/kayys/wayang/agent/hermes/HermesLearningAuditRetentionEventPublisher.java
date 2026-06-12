package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Emits runtime journal events when the learning-audit ledger needs operator attention.
 */
public final class HermesLearningAuditRetentionEventPublisher {

    private static final String SOURCE = "learning-audit-retention";

    private final HermesRuntimeEventSink sink;

    public HermesLearningAuditRetentionEventPublisher(HermesRuntimeEventSink sink) {
        this.sink = sink == null ? HermesRuntimeEventSink.noop() : sink;
    }

    public Optional<HermesRuntimeEvent> publish(HermesLearningAuditRetentionStatus status) {
        return publish(status, Map.of());
    }

    public Optional<HermesRuntimeEvent> publish(
            HermesLearningAuditRetentionStatus status,
            Map<String, Object> eventContext) {
        HermesLearningAuditRetentionStatus resolved = status == null
                ? HermesLearningAuditRetentionStatus.fromMetadata(Map.of())
                : status;
        if (!resolved.requiresAttention()) {
            return Optional.empty();
        }
        HermesRuntimeEvent event = event(resolved, eventContext);
        sink.emit(event);
        return Optional.of(event);
    }

    public static Optional<HermesRuntimeEvent> publish(
            HermesRuntimeEventSink sink,
            HermesLearningAuditRetentionStatus status) {
        return new HermesLearningAuditRetentionEventPublisher(sink).publish(status);
    }

    public static Optional<HermesRuntimeEvent> publish(
            HermesRuntimeEventSink sink,
            HermesLearningAuditRetentionStatus status,
            Map<String, Object> eventContext) {
        return new HermesLearningAuditRetentionEventPublisher(sink).publish(status, eventContext);
    }

    private static HermesRuntimeEvent event(
            HermesLearningAuditRetentionStatus status,
            Map<String, Object> eventContext) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", HermesAgentMode.MODE_ID);
        metadata.put("source", SOURCE);
        metadata.put("retentionStatus", status.toMetadata());
        metadata.put("retentionState", status.status());
        metadata.put("retentionSeverity", status.severity());
        metadata.put("retentionPriority", status.priority());
        metadata.put("retentionRequiresAttention", status.requiresAttention());
        metadata.put("retentionAttention", status.attention());
        metadata.put("retentionRecommendedActions", status.recommendedActions());
        metadata.put("ledgerType", status.ledgerType());
        metadata.put("recordCount", status.recordCount());
        metadata.put("maxEntries", status.maxEntries());
        metadata.put("remainingEntries", status.remainingEntries());
        metadata.put("overflowEntries", status.overflowEntries());
        metadata.put("utilizationPercent", status.utilizationPercent());
        metadata.put("retentionPolicy", status.retentionPolicy());
        putContext(metadata, eventContext);
        return new HermesRuntimeEvent(
                "",
                HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                "",
                "default",
                "",
                "",
                status.status(),
                Instant.now(),
                metadata);
    }

    private static void putContext(
            Map<String, Object> metadata,
            Map<String, Object> eventContext) {
        if (eventContext == null || eventContext.isEmpty()) {
            return;
        }
        eventContext.forEach((key, value) -> {
            String cleanKey = HermesText.oneLineOr(key, "");
            if (!cleanKey.isBlank() && !metadata.containsKey(cleanKey)) {
                metadata.put(cleanKey, value);
            }
        });
    }
}
