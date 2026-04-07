package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Map;

public record TaskCompletedEvent(
        HumanTaskId taskId,
        String completedBy,
        TaskOutcome outcome,
        Map<String, Object> data,
        String comments,
        Instant occurredAt) implements HumanTaskEvent {
}