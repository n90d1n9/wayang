package tech.kayys.gollek.agent.skills.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Audit event record for skill operations.
 *
 * @param eventId unique event identifier
 * @param eventType type of audit event
 * @param timestamp when the event occurred
 * @param userId user who performed the action
 * @param skillId affected skill ID (if applicable)
 * @param action action performed
 * @param status operation status (SUCCESS, FAILURE, DENIED)
 * @param details additional event details
 * @param durationMs operation duration in milliseconds
 * @param ipAddress client IP address
 * @param userAgent client user agent
 */
public record AuditEvent(
    String eventId,
    AuditEventType eventType,
    Instant timestamp,
    String userId,
    String skillId,
    String action,
    AuditStatus status,
    Map<String, Object> details,
    Long durationMs,
    String ipAddress,
    String userAgent
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private AuditEventType eventType;
        private Instant timestamp = Instant.now();
        private String userId = "system";
        private String skillId;
        private String action;
        private AuditStatus status = AuditStatus.SUCCESS;
        private Map<String, Object> details = Map.of();
        private Long durationMs;
        private String ipAddress;
        private String userAgent;

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder eventType(AuditEventType eventType) { this.eventType = eventType; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder skillId(String skillId) { this.skillId = skillId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder status(AuditStatus status) { this.status = status; return this; }
        public Builder details(Map<String, Object> details) { this.details = details; return this; }
        public Builder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }

        public AuditEvent build() {
            return new AuditEvent(
                eventId, eventType, timestamp, userId, skillId, action,
                status, details, durationMs, ipAddress, userAgent
            );
        }
    }
}
