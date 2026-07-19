package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Map;

public record TaskRejectedEvent(
        HumanTaskId taskId,
        String rejectedBy,
        String reason,
        Map<String, Object> data,
        Instant occurredAt) implements HumanTaskEvent {
}