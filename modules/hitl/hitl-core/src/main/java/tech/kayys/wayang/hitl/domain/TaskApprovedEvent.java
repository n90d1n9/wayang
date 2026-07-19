package tech.kayys.wayang.hitl.domain;

import java.time.Instant;
import java.util.Map;

public record TaskApprovedEvent(
        HumanTaskId taskId,
        String approvedBy,
        Map<String, Object> data,
        String comments,
        Instant occurredAt) implements HumanTaskEvent {
}