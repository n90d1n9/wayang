package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Count summary for a returned skill-management event window.
 */
public record SkillManagementEventSummary(
        int totalEvents,
        int successfulEvents,
        int failedEvents,
        Map<String, Integer> operationCounts,
        Map<String, Integer> skillCounts) {

    public SkillManagementEventSummary {
        totalEvents = SkillManagementValueSupport.nonNegative(totalEvents);
        successfulEvents = SkillManagementValueSupport.nonNegative(successfulEvents);
        failedEvents = SkillManagementValueSupport.nonNegative(failedEvents);
        operationCounts = SkillManagementValueSupport.nonNegativeCounts(operationCounts);
        skillCounts = SkillManagementValueSupport.nonNegativeCounts(skillCounts);
    }

    public static SkillManagementEventSummary from(List<SkillManagementEvent> events) {
        List<SkillManagementEvent> values = SkillManagementValueSupport.nonNullList(events);
        TreeMap<String, Integer> operationCounts = new TreeMap<>();
        TreeMap<String, Integer> skillCounts = new TreeMap<>();
        int successfulEvents = 0;
        for (SkillManagementEvent event : values) {
            if (event.success()) {
                successfulEvents++;
            }
            operationCounts.merge(event.operation().name(), 1, Integer::sum);
            if (!event.skillId().isBlank()) {
                skillCounts.merge(event.skillId(), 1, Integer::sum);
            }
        }
        return new SkillManagementEventSummary(
                values.size(),
                successfulEvents,
                values.size() - successfulEvents,
                operationCounts,
                skillCounts);
    }

    public static SkillManagementEventSummary empty() {
        return from(List.of());
    }
}
