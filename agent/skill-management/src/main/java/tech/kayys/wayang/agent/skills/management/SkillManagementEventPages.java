package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Shared query-page assembly for ordered event-history readers.
 */
final class SkillManagementEventPages {

    private SkillManagementEventPages() {
    }

    static SkillManagementEventPage from(
            List<SkillManagementEvent> orderedEvents,
            SkillManagementEventQuery query) {
        SkillManagementEventQuery resolved = query == null ? SkillManagementEventQuery.latest() : query;
        List<SkillManagementEvent> matched = events(orderedEvents).stream()
                .filter(resolved::matches)
                .toList();
        return new SkillManagementEventPage(latestWindow(matched, resolved.limit()), matched.size());
    }

    static List<SkillManagementEvent> latestWindow(
            List<SkillManagementEvent> orderedEvents,
            int limit) {
        List<SkillManagementEvent> events = events(orderedEvents);
        int fromIndex = SkillManagementValueSupport.nonNegative(
                events.size() - SkillManagementValueSupport.nonNegative(limit));
        return List.copyOf(events.subList(fromIndex, events.size()));
    }

    private static List<SkillManagementEvent> events(List<SkillManagementEvent> orderedEvents) {
        return SkillManagementValueSupport.nonNullList(orderedEvents);
    }
}
