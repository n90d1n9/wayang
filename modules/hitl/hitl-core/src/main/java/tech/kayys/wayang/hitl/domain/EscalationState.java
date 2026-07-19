package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

/**
 * EscalationState - Tracks escalation information
 */
public record EscalationState(
        EscalationReason reason,
        String escalatedTo,
        Instant escalatedAt,
        TaskAssignment originalAssignment) {
}