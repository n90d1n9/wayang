package tech.kayys.wayang.agent.skills.management;

/**
 * Synchronization behavior between two skill definition stores.
 */
public record SkillDefinitionStoreSyncOptions(
        boolean overwriteExisting,
        boolean deleteMissingFromTarget,
        boolean dryRun) {

    public static SkillDefinitionStoreSyncOptions bootstrap() {
        return new SkillDefinitionStoreSyncOptions(false, false, false);
    }

    public static SkillDefinitionStoreSyncOptions mirror() {
        return new SkillDefinitionStoreSyncOptions(true, true, false);
    }

    public SkillDefinitionStoreSyncOptions asDryRun() {
        return new SkillDefinitionStoreSyncOptions(overwriteExisting, deleteMissingFromTarget, true);
    }
}
