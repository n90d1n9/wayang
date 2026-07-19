package tech.kayys.wayang.agent.skills.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit record for a skill operation.
 */
public record SkillAuditEvent(
        String eventId,
        SkillAuditEventType eventType,
        String userId,
        String skillId,
        String action,
        SkillAuditStatus status,
        Map<String, Object> details,
        long durationMs,
        Instant occurredAt) {

    public SkillAuditEvent {
        eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        eventType = eventType == null ? SkillAuditEventType.SYSTEM_ERROR : eventType;
        action = action == null ? "" : action;
        status = status == null ? SkillAuditStatus.SUCCESS : status;
        details = details == null ? Map.of() : Map.copyOf(details);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static SkillAuditEvent create(
            SkillAuditEventType eventType,
            String userId,
            String skillId,
            String action,
            SkillAuditStatus status,
            Map<String, Object> details,
            long durationMs) {
        return new SkillAuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                userId,
                skillId,
                action,
                status,
                details,
                durationMs,
                Instant.now());
    }
}
