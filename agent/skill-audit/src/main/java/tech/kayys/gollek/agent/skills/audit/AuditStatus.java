package tech.kayys.gollek.agent.skills.audit;

/**
 * Audit event status.
 */
public enum AuditStatus {
    SUCCESS("Operation completed successfully"),
    FAILURE("Operation failed"),
    DENIED("Access denied"),
    SKIPPED("Operation skipped");

    private final String description;

    AuditStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
