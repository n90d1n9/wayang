package tech.kayys.wayang.hitl.domain;

import java.time.Instant;

// ==================== EVENTS ====================

sealed interface HumanTaskEvent permits
    TaskCreatedEvent,
    TaskAssignedEvent,
    TaskClaimedEvent,
    TaskDelegatedEvent,
    TaskReleasedEvent,
    TaskApprovedEvent,
    TaskRejectedEvent,
    TaskCompletedEvent,
    TaskEscalatedEvent,
    TaskCancelledEvent,
    TaskExpiredEvent,
    TaskCommentAddedEvent {

    HumanTaskId taskId();
    Instant occurredAt();
}