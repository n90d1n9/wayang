package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

public record TaskEscalatedEvent(
        HumanTaskId taskId,
        EscalationReason reason,
        String escalatedTo,
        Instant occurredAt) implements HumanTaskEvent {
}