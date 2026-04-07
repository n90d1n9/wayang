package tech.kayys.wayang.hitl.dto;

import java.time.Instant;

public record AuditEntryDto(
        String entryId,
        String action,
        String details,
        String performedBy,
        Instant timestamp) {
}