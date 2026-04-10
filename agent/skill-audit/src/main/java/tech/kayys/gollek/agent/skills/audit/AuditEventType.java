package tech.kayys.gollek.agent.skills.audit;

import java.time.Instant;

/**
 * Audit event types for skill operations.
 */
public enum AuditEventType {
    // CRUD Operations
    SKILL_CREATED("skill.created", "Skill created"),
    SKILL_UPDATED("skill.updated", "Skill updated"),
    SKILL_DELETED("skill.deleted", "Skill deleted"),
    SKILL_READ("skill.read", "Skill read"),
    
    // Lifecycle
    SKILL_ENABLED("skill.enabled", "Skill enabled"),
    SKILL_DISABLED("skill.disabled", "Skill disabled"),
    
    // Repository Operations
    REPOSITORY_SWITCHED("repository.switched", "Repository switched"),
    REPOSITORY_MIGRATED("repository.migrated", "Repository migrated"),
    REPOSITORY_SYNCED("repository.synced", "Repository synced"),
    
    // Batch Operations
    BATCH_CREATED("batch.created", "Batch skills created"),
    BATCH_DELETED("batch.deleted", "Batch skills deleted"),
    
    // Security
    ACCESS_GRANTED("security.access_granted", "Access granted"),
    ACCESS_DENIED("security.access_denied", "Access denied"),
    VALIDATION_FAILED("security.validation_failed", "Validation failed"),
    
    // System
    CACHE_CLEARED("system.cache_cleared", "Cache cleared"),
    SYSTEM_ERROR("system.error", "System error");

    private final String code;
    private final String description;

    AuditEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
