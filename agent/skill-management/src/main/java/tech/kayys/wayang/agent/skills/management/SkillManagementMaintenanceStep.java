package tech.kayys.wayang.agent.skills.management;

/**
 * Ordered maintenance steps in a combined skill-management maintenance run.
 */
public enum SkillManagementMaintenanceStep {
    DEFINITION_SYNC("definition-sync"),
    ARTIFACT_SYNC("artifact-sync"),
    LIFECYCLE_RECONCILE("lifecycle-reconcile"),
    EVENT_PRUNE("event-prune");

    private final String id;

    SkillManagementMaintenanceStep(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
