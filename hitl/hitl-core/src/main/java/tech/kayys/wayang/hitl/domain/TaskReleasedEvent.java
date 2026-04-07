package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

public record TaskReleasedEvent(
        HumanTaskId taskId,
        String releasedBy,
        Instant occurredAt) implements HumanTaskEvent {
}