package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stable admin-facing projection of one operation and its correlated child events.
 */
public record SkillManagementAdminOperationTrace(
        String operationId,
        boolean rootEventAvailable,
        int totalEvents,
        int successfulEvents,
        int failedEvents,
        int childEventCount,
        boolean healthy,
        boolean failed,
        int failedChildEvents,
        String status,
        SkillManagementAdminEventSummary summary,
        SkillManagementAdminEvent rootEvent,
        List<SkillManagementAdminEvent> childEvents) {

    public SkillManagementAdminOperationTrace(
            String operationId,
            SkillManagementAdminEvent rootEvent,
            List<SkillManagementAdminEvent> childEvents) {
        this(operationId, false, 0, 0, 0, 0, false, false, 0,
                "", SkillManagementAdminEventSummary.empty(), rootEvent, childEvents);
    }

    public SkillManagementAdminOperationTrace {
        operationId = SkillManagementAdminValueSupport.identifier(operationId);
        if (operationId.isBlank() && rootEvent != null) {
            operationId = rootEvent.operationId();
        }
        rootEvent = matchingRoot(operationId, rootEvent);
        childEvents = matchingChildren(operationId, childEvents);
        rootEventAvailable = rootEvent != null;
        childEventCount = childEvents.size();
        totalEvents = childEventCount + (rootEventAvailable ? 1 : 0);
        successfulEvents = countMatching(rootEvent, childEvents, SkillManagementAdminEvent::success);
        failedEvents = totalEvents - successfulEvents;
        failed = failedEvents > 0;
        failedChildEvents = countMatching(null, childEvents, event -> !event.success());
        healthy = rootEventAvailable && !failed;
        status = SkillManagementOperationTraceStatus.from(operationId, rootEventAvailable, failed).name();
        summary = SkillManagementAdminEventSummary.from(events(rootEvent, childEvents));
    }

    private static SkillManagementAdminEvent matchingRoot(
            String operationId,
            SkillManagementAdminEvent rootEvent) {
        if (rootEvent == null || operationId.isBlank()) {
            return null;
        }
        return operationId.equals(rootEvent.operationId()) ? rootEvent : null;
    }

    private static List<SkillManagementAdminEvent> matchingChildren(
            String operationId,
            List<SkillManagementAdminEvent> childEvents) {
        List<SkillManagementAdminEvent> events = SkillManagementAdminValueSupport.nonNullList(childEvents);
        if (operationId.isBlank()) {
            return List.of();
        }
        return events.stream()
                .filter(event -> operationId.equals(event.parentOperationId()))
                .toList();
    }

    private static int countMatching(
            SkillManagementAdminEvent rootEvent,
            List<SkillManagementAdminEvent> childEvents,
            Predicate<SkillManagementAdminEvent> predicate) {
        int matches = rootEvent != null && predicate.test(rootEvent) ? 1 : 0;
        matches += SkillManagementAdminValueSupport.countMatching(childEvents, predicate);
        return matches;
    }

    private static List<SkillManagementAdminEvent> events(
            SkillManagementAdminEvent rootEvent,
            List<SkillManagementAdminEvent> childEvents) {
        ArrayList<SkillManagementAdminEvent> events = new ArrayList<>();
        if (rootEvent != null) {
            events.add(rootEvent);
        }
        events.addAll(childEvents);
        return List.copyOf(events);
    }
}
