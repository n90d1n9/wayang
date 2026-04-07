package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

public record TaskAssignedEvent(
        HumanTaskId taskId,
        TaskAssignment assignment,
        Instant occurredAt) implements HumanTaskEvent {
}