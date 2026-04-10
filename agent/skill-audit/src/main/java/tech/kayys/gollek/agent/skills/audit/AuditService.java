package tech.kayys.gollek.agent.skills.audit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Audit service for logging skill operations.
 *
 * <p>Features:
 * <ul>
 *   <li>Asynchronous audit logging</li>
 *   <li>Event-driven architecture</li>
 *   <li>File and database storage support</li>
 *   <li>Configurable audit levels</li>
 *   <li>Audit trail for compliance</li>
 * </ul>
 */
@ApplicationScoped
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Inject
    Event<AuditEvent> auditEventSink;

    private AuditLevel auditLevel = AuditLevel.ALL;

    /**
     * Log an audit event.
     *
     * @param eventType type of event
     * @param userId user ID
     * @param skillId skill ID
     * @param action action performed
     * @param status operation status
     */
    public void log(AuditEventType eventType, String userId, String skillId, 
                   String action, AuditStatus status) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .skillId(skillId)
                .action(action)
                .status(status)
                .build();

        fireAuditEvent(event);
    }

    /**
     * Log an audit event with details.
     *
     * @param eventType type of event
     * @param userId user ID
     * @param skillId skill ID
     * @param action action performed
     * @param status operation status
     * @param details additional details
     */
    public void log(AuditEventType eventType, String userId, String skillId,
                   String action, AuditStatus status, java.util.Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .skillId(skillId)
                .action(action)
                .status(status)
                .details(details)
                .build();

        fireAuditEvent(event);
    }

    /**
     * Log an audit event with duration.
     *
     * @param eventType type of event
     * @param userId user ID
     * @param skillId skill ID
     * @param action action performed
     * @param status operation status
     * @param durationMs operation duration
     */
    public void logWithDuration(AuditEventType eventType, String userId, String skillId,
                               String action, AuditStatus status, long durationMs) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .skillId(skillId)
                .action(action)
                .status(status)
                .durationMs(durationMs)
                .build();

        fireAuditEvent(event);
    }

    /**
     * Log successful operation.
     */
    public void logSuccess(AuditEventType eventType, String userId, String skillId, String action) {
        log(eventType, userId, skillId, action, AuditStatus.SUCCESS);
    }

    /**
     * Log failed operation.
     */
    public void logFailure(AuditEventType eventType, String userId, String skillId, 
                          String action, String error) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .skillId(skillId)
                .action(action)
                .status(AuditStatus.FAILURE)
                .details(java.util.Map.of("error", error))
                .build();

        fireAuditEvent(event);
    }

    /**
     * Log access denied.
     */
    public void logAccessDenied(String userId, String skillId, String reason) {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(AuditEventType.ACCESS_DENIED)
                .userId(userId)
                .skillId(skillId)
                .action("access")
                .status(AuditStatus.DENIED)
                .details(java.util.Map.of("reason", reason))
                .build();

        fireAuditEvent(event);
    }

    /**
     * Set audit level.
     */
    public void setAuditLevel(AuditLevel level) {
        this.auditLevel = level;
        log.info("Audit level set to: {}", level);
    }

    private void fireAuditEvent(AuditEvent event) {
        if (shouldLog(event)) {
            try {
                auditEventSink.fireAsync(event);
                log.debug("Audit event fired: {} - {}", event.eventId(), event.eventType());
            } catch (Exception e) {
                log.error("Failed to fire audit event", e);
            }
        }
    }

    private boolean shouldLog(AuditEvent event) {
        return switch (auditLevel) {
            case ALL -> true;
            case CHANGES -> event.eventType() == AuditEventType.SKILL_CREATED ||
                          event.eventType() == AuditEventType.SKILL_UPDATED ||
                          event.eventType() == AuditEventType.SKILL_DELETED;
            case SECURITY -> event.eventType() == AuditEventType.ACCESS_DENIED ||
                           event.eventType() == AuditEventType.VALIDATION_FAILED ||
                           event.status() == AuditStatus.FAILURE;
            case NONE -> false;
        };
    }

    /**
     * Audit level configuration.
     */
    public enum AuditLevel {
        ALL("Log all events"),
        CHANGES("Log only create/update/delete"),
        SECURITY("Log only security events"),
        NONE("Disable audit logging");

        private final String description;

        AuditLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
