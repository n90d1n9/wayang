package tech.kayys.wayang.agent.skills.management;

/**
 * Maintenance policy for optional event-history pruning.
 */
public record SkillManagementEventPrunePolicy(
        boolean enabled,
        SkillManagementEventPruneOptions options) {

    public SkillManagementEventPrunePolicy {
        options = options == null
                ? SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)
                : options;
    }

    public static SkillManagementEventPrunePolicy disabled() {
        return new SkillManagementEventPrunePolicy(
                false,
                SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS));
    }

    public static SkillManagementEventPrunePolicy keepLatest(int keepLatestEvents) {
        return new SkillManagementEventPrunePolicy(
                true,
                SkillManagementEventPruneOptions.keepLatest(keepLatestEvents));
    }

    public static SkillManagementEventPrunePolicy dryRun(int keepLatestEvents) {
        return new SkillManagementEventPrunePolicy(
                true,
                SkillManagementEventPruneOptions.dryRun(keepLatestEvents));
    }

    public SkillManagementEventPrunePolicy asDryRun() {
        if (!enabled) {
            return this;
        }
        return dryRun(options.keepLatestEvents());
    }
}
