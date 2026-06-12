package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Shared bounded-retention calculations for ordered event-history stores.
 */
final class SkillManagementEventRetention {

    private SkillManagementEventRetention() {
    }

    static SkillManagementEventPruneOptions resolve(
            SkillManagementEventPruneOptions options,
            int defaultKeepLatestEvents) {
        return options == null
                ? SkillManagementEventPruneOptions.keepLatest(defaultKeepLatestEvents)
                : options;
    }

    static int normalizeCapacity(int maxEvents) {
        return SkillManagementValueSupport.atLeast(maxEvents, 1);
    }

    static int normalizeCapacityOrDefault(int maxEvents, int defaultMaxEvents) {
        return maxEvents <= 0
                ? normalizeCapacity(defaultMaxEvents)
                : normalizeCapacity(maxEvents);
    }

    static int removableCount(int scannedEvents, int keepLatestEvents) {
        return SkillManagementValueSupport.nonNegative(
                SkillManagementValueSupport.nonNegative(scannedEvents)
                        - SkillManagementValueSupport.nonNegative(keepLatestEvents));
    }

    static <T> List<T> oldestToPrune(List<T> orderedEvents, int keepLatestEvents) {
        List<T> events = SkillManagementValueSupport.nonNullList(orderedEvents);
        int removable = removableCount(events.size(), keepLatestEvents);
        return List.copyOf(events.subList(0, removable));
    }
}
