package tech.kayys.wayang.agent.skills.audit;

import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory audit trail for skill operations.
 */
public class SkillAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(SkillAuditService.class);

    private final ConcurrentLinkedDeque<SkillAuditEvent> events = new ConcurrentLinkedDeque<>();
    private volatile SkillAuditLevel auditLevel = SkillAuditLevel.ALL;

    public Uni<SkillAuditEvent> log(
            SkillAuditEventType eventType,
            String userId,
            String skillId,
            String action,
            SkillAuditStatus status) {
        return log(eventType, userId, skillId, action, status, Map.of(), 0);
    }

    public Uni<SkillAuditEvent> log(
            SkillAuditEventType eventType,
            String userId,
            String skillId,
            String action,
            SkillAuditStatus status,
            Map<String, Object> details,
            long durationMs) {
        return Uni.createFrom().item(() -> {
            SkillAuditEvent event = SkillAuditEvent.create(
                    eventType,
                    userId,
                    skillId,
                    action,
                    status,
                    details,
                    durationMs);
            if (shouldLog(event)) {
                events.add(event);
                LOG.debug("Recorded skill audit event: {} {}", event.eventType(), event.skillId());
            }
            return event;
        });
    }

    public Uni<SkillAuditEvent> logSuccess(
            SkillAuditEventType eventType,
            String userId,
            String skillId,
            String action) {
        return log(eventType, userId, skillId, action, SkillAuditStatus.SUCCESS);
    }

    public Uni<SkillAuditEvent> logFailure(
            SkillAuditEventType eventType,
            String userId,
            String skillId,
            String action,
            String error) {
        return log(
                eventType,
                userId,
                skillId,
                action,
                SkillAuditStatus.FAILURE,
                Map.of("error", error == null ? "" : error),
                0);
    }

    public Uni<SkillAuditEvent> logAccessDenied(String userId, String skillId, String reason) {
        return log(
                SkillAuditEventType.ACCESS_DENIED,
                userId,
                skillId,
                "access",
                SkillAuditStatus.DENIED,
                Map.of("reason", reason == null ? "" : reason),
                0);
    }

    public Uni<List<SkillAuditEvent>> latestEvents(int limit) {
        return Uni.createFrom().item(() -> events.stream()
                .sorted(Comparator.comparing(SkillAuditEvent::occurredAt).reversed())
                .limit(Math.max(0, limit))
                .toList());
    }

    public Uni<List<SkillAuditEvent>> eventsForSkill(String skillId) {
        return Uni.createFrom().item(() -> events.stream()
                .filter(event -> java.util.Objects.equals(skillId, event.skillId()))
                .sorted(Comparator.comparing(SkillAuditEvent::occurredAt))
                .toList());
    }

    public Uni<List<SkillAuditEvent>> eventsForUser(String userId) {
        return Uni.createFrom().item(() -> events.stream()
                .filter(event -> java.util.Objects.equals(userId, event.userId()))
                .sorted(Comparator.comparing(SkillAuditEvent::occurredAt))
                .toList());
    }

    public void setAuditLevel(SkillAuditLevel auditLevel) {
        this.auditLevel = auditLevel == null ? SkillAuditLevel.ALL : auditLevel;
    }

    public SkillAuditLevel auditLevel() {
        return auditLevel;
    }

    public int size() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }

    private boolean shouldLog(SkillAuditEvent event) {
        return switch (auditLevel) {
            case ALL -> true;
            case CHANGES -> event.eventType() == SkillAuditEventType.SKILL_CREATED
                    || event.eventType() == SkillAuditEventType.SKILL_UPDATED
                    || event.eventType() == SkillAuditEventType.SKILL_DELETED
                    || event.eventType() == SkillAuditEventType.SKILL_ENABLED
                    || event.eventType() == SkillAuditEventType.SKILL_DISABLED
                    || event.eventType() == SkillAuditEventType.SKILL_DEPRECATED;
            case SECURITY -> event.eventType() == SkillAuditEventType.ACCESS_DENIED
                    || event.eventType() == SkillAuditEventType.VALIDATION_FAILED
                    || event.status() == SkillAuditStatus.FAILURE
                    || event.status() == SkillAuditStatus.DENIED;
            case NONE -> false;
        };
    }
}
