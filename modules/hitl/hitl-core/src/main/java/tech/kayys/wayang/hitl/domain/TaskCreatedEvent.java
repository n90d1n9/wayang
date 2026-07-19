package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

public record TaskCreatedEvent(
        HumanTaskId taskId,
        String workflowRunId,
        String nodeId,
        Instant occurredAt) implements HumanTaskEvent {
}