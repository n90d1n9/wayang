package tech.kayys.gollek.agent.skills.audit.rbac;

/**
 * Permission types for skill operations.
 */
public enum PermissionType {
    // CRUD
    SKILL_CREATE("Create new skills"),
    SKILL_READ("Read skills"),
    SKILL_UPDATE("Update skills"),
    SKILL_DELETE("Delete skills"),
    
    // Lifecycle
    SKILL_ENABLE("Enable skills"),
    SKILL_DISABLE("Disable skills"),
    
    // Repository
    REPOSITORY_SWITCH("Switch repositories"),
    REPOSITORY_MIGRATE("Migrate repositories"),
    
    // Batch
    BATCH_CREATE("Batch create skills"),
    BATCH_DELETE("Batch delete skills"),
    
    // Admin
    AUDIT_READ("Read audit logs"),
    CACHE_CLEAR("Clear cache"),
    ADMIN_ACCESS("Administrative access");

    private final String description;

    PermissionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
