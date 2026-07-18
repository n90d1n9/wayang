package tech.kayys.wayang.agent.skills.management;

/**
 * Options for bounded skill-management event-history pruning.
 */
public record SkillManagementEventPruneOptions(
        int keepLatestEvents,
        boolean dryRun) {

    public SkillManagementEventPruneOptions {
        keepLatestEvents = SkillManagementValueSupport.nonNegative(keepLatestEvents);
    }

    public static SkillManagementEventPruneOptions keepLatest(int keepLatestEvents) {
        return new SkillManagementEventPruneOptions(keepLatestEvents, false);
    }

    public static SkillManagementEventPruneOptions dryRun(int keepLatestEvents) {
        return new SkillManagementEventPruneOptions(keepLatestEvents, true);
    }
}
