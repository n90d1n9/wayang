package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stable admin-facing projection of skill-management event counts.
 */
public record SkillManagementAdminEventSummary(
        int totalEvents,
        int successfulEvents,
        int failedEvents,
        Map<String, Integer> operationCounts,
        Map<String, Integer> skillCounts) {

    public SkillManagementAdminEventSummary {
        totalEvents = SkillManagementAdminValueSupport.nonNegative(totalEvents);
        successfulEvents = SkillManagementAdminValueSupport.nonNegative(successfulEvents);
        failedEvents = SkillManagementAdminValueSupport.nonNegative(failedEvents);
        operationCounts = SkillManagementAdminValueSupport.nonNegativeCounts(operationCounts);
        skillCounts = SkillManagementAdminValueSupport.nonNegativeCounts(skillCounts);
    }

    static SkillManagementAdminEventSummary from(List<SkillManagementAdminEvent> events) {
        List<SkillManagementAdminEvent> values = SkillManagementAdminValueSupport.nonNullList(events);
        TreeMap<String, Integer> operationCounts = new TreeMap<>();
        TreeMap<String, Integer> skillCounts = new TreeMap<>();
        int successfulEvents = 0;
        for (SkillManagementAdminEvent event : values) {
            if (event.success()) {
                successfulEvents++;
            }
            operationCounts.merge(event.operation(), 1, Integer::sum);
            if (!event.skillId().isBlank()) {
                skillCounts.merge(event.skillId(), 1, Integer::sum);
            }
        }
        return new SkillManagementAdminEventSummary(
                values.size(),
                successfulEvents,
                values.size() - successfulEvents,
                operationCounts,
                skillCounts);
    }

    static SkillManagementAdminEventSummary empty() {
        return from(List.of());
    }
}
