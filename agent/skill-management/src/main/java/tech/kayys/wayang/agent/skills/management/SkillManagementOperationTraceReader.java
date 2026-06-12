package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Reads correlated operation telemetry from event history.
 */
final class SkillManagementOperationTraceReader {

    private final SkillManagementEventHistory eventHistory;

    SkillManagementOperationTraceReader(SkillManagementEventHistory eventHistory) {
        this.eventHistory = Objects.requireNonNull(eventHistory, "eventHistory");
    }

    SkillManagementAdminOperationTrace trace(String operationId, int limit) {
        String resolvedOperationId = SkillManagementValueSupport.identifier(operationId);
        if (resolvedOperationId.isBlank()) {
            return new SkillManagementAdminOperationTrace("", null, List.of());
        }
        SkillManagementEventPage rootPage = eventHistory.query(
                SkillManagementEventQuery.forOperationId(resolvedOperationId, 1));
        SkillManagementEventPage childPage = eventHistory.query(
                SkillManagementEventQuery.forParentOperationId(resolvedOperationId, limit));
        return SkillManagementAdminOperationTraceViews.operationTrace(resolvedOperationId, rootPage, childPage);
    }

    SkillManagementAdminOperationTracePage deploymentTraces(int operationLimit, int childEventLimit) {
        return deploymentTraces(SkillManagementOperationTraceQuery.deployments(operationLimit, childEventLimit));
    }

    SkillManagementAdminOperationTracePage deploymentTraces(SkillManagementOperationTraceQuery query) {
        SkillManagementOperationTraceQuery resolved =
                query == null ? SkillManagementOperationTraceQuery.deployments(
                        SkillManagementEventQuery.DEFAULT_LIMIT,
                        SkillManagementEventQuery.DEFAULT_LIMIT) : query;
        SkillManagementEventPage rootPage = eventHistory.query(
                SkillManagementEventQuery.deployments(resolved.operationLimit()));
        List<String> rootOperationIds = rootPage.events().stream()
                .map(event -> SkillManagementEventAttributeReader.from(event).operationId())
                .toList();
        int untraceableRootEvents = SkillManagementAdminValueSupport.countMatching(
                rootOperationIds,
                String::isBlank);
        List<String> traceableOperationIds = rootOperationIds.stream()
                .filter(operationId -> !operationId.isBlank())
                .distinct()
                .toList();
        List<SkillManagementAdminOperationTrace> candidateTraces = traceableOperationIds.stream()
                .map(operationId -> trace(operationId, resolved.childEventLimit()))
                .toList();
        List<SkillManagementAdminOperationTrace> traces = candidateTraces.stream()
                .filter(resolved::matches)
                .toList();
        return new SkillManagementAdminOperationTracePage(
                rootPage.matchedEvents(),
                rootPage.returnedEvents(),
                traceableOperationIds.size(),
                untraceableRootEvents,
                candidateTraces.size() - traces.size(),
                rootPage.truncated(),
                resolved.childEventLimit(),
                resolved.statusFilter(),
                traces);
    }
}
