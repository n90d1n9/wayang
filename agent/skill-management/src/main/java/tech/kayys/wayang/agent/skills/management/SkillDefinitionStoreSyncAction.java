package tech.kayys.wayang.agent.skills.management;

/**
 * Action produced while synchronizing skill definition stores.
 */
public enum SkillDefinitionStoreSyncAction {
    COPIED,
    UPDATED,
    UNCHANGED,
    CONFLICT,
    DELETED
}
