package tech.kayys.wayang.agent.skills.management;

/**
 * Compact status for an individual maintenance step.
 */
public enum SkillManagementMaintenanceStepStatus {
    SKIPPED,
    DRY_RUN,
    CHANGED,
    UNCHANGED,
    CONFLICT,
    INCONSISTENT,
    FAILED
}
