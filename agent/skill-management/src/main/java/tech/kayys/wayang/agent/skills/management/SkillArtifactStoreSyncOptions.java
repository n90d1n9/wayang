package tech.kayys.wayang.agent.skills.management;

/**
 * Synchronization behavior between two skill artifact stores.
 */
public record SkillArtifactStoreSyncOptions(
        boolean overwriteExisting,
        boolean deleteMissingFromTarget,
        boolean dryRun) {

    public static SkillArtifactStoreSyncOptions bootstrap() {
        return new SkillArtifactStoreSyncOptions(false, false, false);
    }

    public static SkillArtifactStoreSyncOptions mirror() {
        return new SkillArtifactStoreSyncOptions(true, true, false);
    }

    public SkillArtifactStoreSyncOptions asDryRun() {
        return new SkillArtifactStoreSyncOptions(overwriteExisting, deleteMissingFromTarget, true);
    }
}
