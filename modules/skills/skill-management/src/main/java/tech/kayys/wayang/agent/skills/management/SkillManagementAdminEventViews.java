package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Maps skill-management event history to stable admin DTOs.
 */
final class SkillManagementAdminEventViews {

    private SkillManagementAdminEventViews() {
    }

    static SkillManagementAdminEventPage eventPage(SkillManagementEventPage page) {
        Objects.requireNonNull(page, "page");
        return new SkillManagementAdminEventPage(
                page.matchedEvents(),
                page.returnedEvents(),
                page.truncated(),
                eventSummary(page.summary()),
                page.events().stream()
                        .map(SkillManagementAdminEventViews::event)
                        .toList());
    }

    static SkillManagementAdminEvent event(SkillManagementEvent event) {
        Objects.requireNonNull(event, "event");
        SkillManagementEventAttributeReader attributes = SkillManagementEventAttributeReader.from(event);
        return new SkillManagementAdminEvent(
                event.occurredAt().toString(),
                event.operation().name(),
                event.skillId(),
                attributes.operationId(),
                attributes.parentOperationId(),
                event.success(),
                event.attributes());
    }

    static SkillManagementAdminEventSummary eventSummary(SkillManagementEventSummary summary) {
        Objects.requireNonNull(summary, "summary");
        return new SkillManagementAdminEventSummary(
                summary.totalEvents(),
                summary.successfulEvents(),
                summary.failedEvents(),
                summary.operationCounts(),
                summary.skillCounts());
    }
}
