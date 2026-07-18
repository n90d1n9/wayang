package tech.kayys.wayang.agent.skills.management;

/**
 * Action produced while synchronizing skill artifact stores.
 */
public enum SkillArtifactStoreSyncAction {
    COPIED,
    UPDATED,
    UNCHANGED,
    CONFLICT,
    DELETED
}
