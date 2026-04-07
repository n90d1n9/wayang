package tech.kayys.wayang.agent.spi.core;

/**
 * Coordination Status
 */
public enum CoordinationStatus {
    INITIATED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    CANCELLED
}
