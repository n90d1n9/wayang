package tech.kayys.wayang.agent.skills.management;

/**
 * Stable operation names emitted by skill-management observability hooks.
 */
public enum SkillManagementEventOperation {
    CREATE_SKILL,
    UPDATE_SKILL,
    DELETE_SKILL,
    TRANSITION_SKILL,
    PUT_ARTIFACT,
    DELETE_ARTIFACT,
    SYNC_ARTIFACTS,
    RECONCILE_LIFECYCLE,
    BOOTSTRAP,
    DEPLOYMENT,
    MAINTENANCE
}
