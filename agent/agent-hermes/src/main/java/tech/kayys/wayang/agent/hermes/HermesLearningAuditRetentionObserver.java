package tech.kayys.wayang.agent.hermes;

import java.util.Map;
import java.util.Optional;

/**
 * Runtime observer that records learning-audit retention transitions after learning writes.
 */
public final class HermesLearningAuditRetentionObserver {

    private final HermesLearningAuditService auditService;
    private final HermesLearningAuditRetentionEventMonitor monitor;
    private volatile HermesLearningAuditRetentionObservation lastObservation =
            HermesLearningAuditRetentionObservation.unavailable("not-observed");

    public HermesLearningAuditRetentionObserver(
            HermesLearningAuditService auditService,
            HermesLearningAuditRetentionEventMonitor monitor) {
        this.auditService = auditService;
        this.monitor = monitor;
    }

    public Optional<HermesRuntimeEvent> observe() {
        return observeRetention().event();
    }

    public HermesLearningAuditRetentionObservation observeRetention() {
        HermesLearningAuditRetentionObservation observation;
        if (auditService == null || monitor == null) {
            observation = HermesLearningAuditRetentionObservation.unavailable("observer-unavailable");
            lastObservation = observation;
            return observation;
        }
        try {
            observation = auditService.observeRetention(monitor);
        } catch (RuntimeException error) {
            observation = HermesLearningAuditRetentionObservation.failed(error);
        }
        lastObservation = observation;
        return observation;
    }

    public HermesLearningAuditRetentionObservation lastObservation() {
        return lastObservation;
    }

    public Map<String, Object> toMetadata() {
        return lastObservation.toMetadata();
    }

    public static HermesLearningAuditRetentionObserver noop() {
        return new HermesLearningAuditRetentionObserver(null, null);
    }
}
