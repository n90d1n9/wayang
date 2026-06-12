package tech.kayys.wayang.agent.skills.management;

/**
 * Raised when a multi-store skill-management write cannot be completed cleanly.
 */
public final class SkillManagementWriteException extends IllegalStateException {

    public SkillManagementWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
