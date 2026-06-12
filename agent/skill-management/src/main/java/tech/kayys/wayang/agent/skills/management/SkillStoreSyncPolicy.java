package tech.kayys.wayang.agent.skills.management;

/**
 * Package-neutral sync behavior shared by definition and artifact store config parsing.
 */
record SkillStoreSyncPolicy(
        boolean overwriteExisting,
        boolean deleteMissingFromTarget,
        boolean dryRun) {

    static SkillStoreSyncPolicy of(
            boolean overwriteExisting,
            boolean deleteMissingFromTarget,
            boolean dryRun) {
        return new SkillStoreSyncPolicy(overwriteExisting, deleteMissingFromTarget, dryRun);
    }

    static SkillStoreSyncPolicy bootstrap() {
        return new SkillStoreSyncPolicy(false, false, false);
    }

    static SkillStoreSyncPolicy mirror() {
        return new SkillStoreSyncPolicy(true, true, false);
    }

    SkillStoreSyncPolicy asDryRun() {
        return new SkillStoreSyncPolicy(overwriteExisting, deleteMissingFromTarget, true);
    }
}
