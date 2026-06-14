package tech.kayys.wayang.agent.analytics;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a skill usage event for analytics tracking.
 */
public record SkillUsageEvent(
    String eventId,
    String skillId,
    String tenantId,
    String userId,
    EventType eventType,
    Instant timestamp,
    long durationMs,
    boolean successful,
    String errorMessage,
    Map<String, Object> metadata
) {
    
    public enum EventType {
        EXECUTION_STARTED,
        EXECUTION_COMPLETED,
        EXECUTION_FAILED,
        SKILL_LOADED,
        SKILL_UNLOADED,
        CACHE_HIT,
        CACHE_MISS
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String eventId;
        private String skillId;
        private String tenantId;
        private String userId;
        private EventType eventType;
        private Instant timestamp;
        private long durationMs;
        private boolean successful = true;
        private String errorMessage;
        private Map<String, Object> metadata;
        
        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public SkillUsageEvent build() {
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new SkillUsageEvent(
                eventId, skillId, tenantId, userId, eventType,
                timestamp, durationMs, successful, errorMessage, metadata
            );
        }
    }
}
