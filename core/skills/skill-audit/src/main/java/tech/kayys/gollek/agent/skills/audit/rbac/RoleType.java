package tech.kayys.gollek.agent.skills.audit.rbac;

/**
 * Role types for skill access control.
 */
public enum RoleType {
    ADMIN("Full access to all operations"),
    DEVELOPER("Create, update, delete own skills"),
    USER("Read and execute skills"),
    VIEWER("Read-only access"),
    AUDITOR("Read audit logs");

    private final String description;

    RoleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
