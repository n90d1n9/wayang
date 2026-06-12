package tech.kayys.wayang.agent.skills.audit;

/**
 * Audit event types for skill lifecycle and runtime operations.
 */
public enum SkillAuditEventType {
    SKILL_CREATED,
    SKILL_UPDATED,
    SKILL_DELETED,
    SKILL_ENABLED,
    SKILL_DISABLED,
    SKILL_DEPRECATED,
    SKILL_EXECUTED,
    ACCESS_DENIED,
    VALIDATION_FAILED,
    SYSTEM_ERROR
}
