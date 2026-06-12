package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Stable admin-facing projection of a bounded skill-management event page.
 */
public record SkillManagementAdminEventPage(
        int matchedEvents,
        int returnedEvents,
        boolean truncated,
        SkillManagementAdminEventSummary summary,
        List<SkillManagementAdminEvent> events) {

    public SkillManagementAdminEventPage {
        matchedEvents = SkillManagementAdminValueSupport.nonNegative(matchedEvents);
        events = SkillManagementAdminValueSupport.nonNullList(events);
        returnedEvents = SkillManagementAdminValueSupport.nonNegative(returnedEvents);
        if (returnedEvents != events.size()) {
            returnedEvents = events.size();
        }
        truncated = truncated || matchedEvents > returnedEvents;
        summary = Objects.requireNonNull(summary, "summary");
    }
}
