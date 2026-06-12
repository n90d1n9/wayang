package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Bounded event query result with matched and returned-window metadata.
 */
public record SkillManagementEventPage(
        List<SkillManagementEvent> events,
        int matchedEvents) {

    public SkillManagementEventPage {
        events = SkillManagementValueSupport.nonNullList(events);
        matchedEvents = SkillManagementValueSupport.atLeast(matchedEvents, events.size());
    }

    public int returnedEvents() {
        return events.size();
    }

    public boolean truncated() {
        return matchedEvents > returnedEvents();
    }

    public SkillManagementEventSummary summary() {
        return SkillManagementEventSummary.from(events);
    }
}
