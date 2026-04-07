package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

public record TaskDelegatedEvent(
        HumanTaskId taskId,
        String fromUser,
        String toUser,
        String reason,
        Instant occurredAt) implements HumanTaskEvent {
}