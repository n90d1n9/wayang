package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

/**
 * TaskAuditEntry - Audit trail entry
 */
public record TaskAuditEntry(
        String entryId,
        String action,
        String details,
        String performedBy,
        Instant timestamp) {
}