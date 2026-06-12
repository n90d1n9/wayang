package tech.kayys.wayang.agent.skills.management;

/**
 * Named service-level persistence profiles for common skill-management deployments.
 */
public enum SkillManagementServiceProfile {
    DEFAULT("default"),
    LOCAL_FILESYSTEM("local-filesystem"),
    OBJECT_STORAGE("object-storage"),
    JDBC("jdbc"),
    HYBRID_OBJECT_FILE("hybrid-object-file"),
    MIRRORED_OBJECT_FILE("mirrored-object-file");

    private final String label;

    SkillManagementServiceProfile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
