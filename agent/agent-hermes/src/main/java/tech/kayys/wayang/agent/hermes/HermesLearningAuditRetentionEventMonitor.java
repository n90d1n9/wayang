package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transition-aware retention watcher that prevents repeated learning-audit alerts.
 */
public final class HermesLearningAuditRetentionEventMonitor {

    private static final RetentionFingerprint NONE =
            new RetentionFingerprint("", "", "", 0, 0, false);

    private final HermesLearningAuditRetentionEventPublisher publisher;
    private RetentionFingerprint lastPublished = NONE;

    public HermesLearningAuditRetentionEventMonitor(HermesRuntimeEventSink sink) {
        this(new HermesLearningAuditRetentionEventPublisher(sink));
    }

    public HermesLearningAuditRetentionEventMonitor(HermesLearningAuditRetentionEventPublisher publisher) {
        this.publisher = publisher == null
                ? new HermesLearningAuditRetentionEventPublisher(HermesRuntimeEventSink.noop())
                : publisher;
    }

    public synchronized Optional<HermesRuntimeEvent> publishIfChanged(
            HermesLearningAuditRetentionStatus status) {
        return observe(status).event();
    }

    public synchronized HermesLearningAuditRetentionObservation observe(
            HermesLearningAuditRetentionStatus status) {
        HermesLearningAuditRetentionStatus resolved = status == null
                ? HermesLearningAuditRetentionStatus.fromMetadata(Map.of())
                : status;
        RetentionFingerprint current = RetentionFingerprint.from(resolved);
        RetentionFingerprint previous = lastPublished;
        if (!resolved.requiresAttention()) {
            lastPublished = NONE;
            String reason = previous.active() ? "recovered" : "not-pressured";
            return HermesLearningAuditRetentionObservation.suppressed(
                    resolved,
                    reason,
                    nonAttentionContext(previous, resolved, reason));
        }
        if (current.equals(previous)) {
            return HermesLearningAuditRetentionObservation.suppressed(
                    resolved,
                    "duplicate-state",
                    transitionContext(previous, current));
        }
        Optional<HermesRuntimeEvent> event = publisher.publish(resolved, transitionContext(previous, current));
        event.ifPresent(ignored -> lastPublished = current);
        return event
                .map(runtimeEvent -> HermesLearningAuditRetentionObservation.emitted(
                        resolved,
                        runtimeEvent,
                        transitionReason(previous),
                        transitionContext(previous, current)))
                .orElseGet(() -> HermesLearningAuditRetentionObservation.suppressed(
                        resolved,
                        "publisher-suppressed",
                        transitionContext(previous, current)));
    }

    private static Map<String, Object> transitionContext(
            RetentionFingerprint previous,
            RetentionFingerprint current) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retentionEventReason", transitionReason(previous));
        metadata.put("previousRetentionState", previous.status());
        metadata.put("previousRetentionSeverity", previous.severity());
        metadata.put("previousRetentionPriority", previous.priority());
        metadata.put("currentRetentionState", current.status());
        metadata.put("currentRetentionSeverity", current.severity());
        metadata.put("currentRetentionPriority", current.priority());
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> nonAttentionContext(
            RetentionFingerprint previous,
            HermesLearningAuditRetentionStatus current,
            String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retentionEventReason", reason);
        metadata.put("previousRetentionState", previous.status());
        metadata.put("previousRetentionSeverity", previous.severity());
        metadata.put("previousRetentionPriority", previous.priority());
        metadata.put("currentRetentionState", current.status());
        metadata.put("currentRetentionSeverity", current.severity());
        metadata.put("currentRetentionPriority", current.priority());
        return Map.copyOf(metadata);
    }

    private static String transitionReason(RetentionFingerprint previous) {
        return previous.active() ? "state-transition" : "first-observation";
    }

    private record RetentionFingerprint(
            String ledgerType,
            String status,
            String severity,
            int priority,
            int maxEntries,
            boolean active) {

        static RetentionFingerprint from(HermesLearningAuditRetentionStatus status) {
            if (status == null || !status.requiresAttention()) {
                return NONE;
            }
            return new RetentionFingerprint(
                    status.ledgerType(),
                    status.status(),
                    status.severity(),
                    status.priority(),
                    status.maxEntries(),
                    true);
        }
    }
}
