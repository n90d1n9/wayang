package tech.kayys.wayang.agent.skills.management;

/**
 * Controls how lifecycle state reconciliation mutates the state store.
 */
public record SkillLifecycleStateReconcileOptions(
        boolean createMissingStates,
        boolean removeOrphanedStates) {

    public static SkillLifecycleStateReconcileOptions inspectOnly() {
        return new SkillLifecycleStateReconcileOptions(false, false);
    }

    public static SkillLifecycleStateReconcileOptions createMissing() {
        return new SkillLifecycleStateReconcileOptions(true, false);
    }

    public static SkillLifecycleStateReconcileOptions createMissingAndRemoveOrphans() {
        return new SkillLifecycleStateReconcileOptions(true, true);
    }
}
