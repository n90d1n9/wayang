package tech.kayys.wayang.hitl.dto;

import java.time.Instant;

public record AssignmentHistoryDto(
        String assigneeType,
        String assigneeIdentifier,
        String assignedBy,
        Instant assignedAt,
        String delegationReason) {
}