package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of recent operation traces.
 */
public record SkillManagementAdminOperationTracePage(
        int matchedRootEvents,
        int returnedRootEvents,
        int traceableRootEvents,
        int untraceableRootEvents,
        int filteredTraces,
        int returnedTraces,
        boolean truncated,
        int childEventLimit,
        int healthyTraces,
        int failedTraces,
        int rootMissingTraces,
        int missingOperationIdTraces,
        String statusFilter,
        List<SkillManagementAdminOperationTrace> traces) {

    public SkillManagementAdminOperationTracePage(
            int matchedRootEvents,
            int childEventLimit,
            List<SkillManagementAdminOperationTrace> traces) {
        this(matchedRootEvents, childEventLimit, "", traces);
    }

    public SkillManagementAdminOperationTracePage(
            int matchedRootEvents,
            int childEventLimit,
            String statusFilter,
            List<SkillManagementAdminOperationTrace> traces) {
        this(matchedRootEvents, inferredTruncated(matchedRootEvents, traces),
                childEventLimit, statusFilter, traces);
    }

    public SkillManagementAdminOperationTracePage(
            int matchedRootEvents,
            boolean truncated,
            int childEventLimit,
            String statusFilter,
            List<SkillManagementAdminOperationTrace> traces) {
        this(matchedRootEvents, traceCount(traces), traceCount(traces), 0, 0,
                0, truncated, childEventLimit, 0, 0, 0, 0, statusFilter, traces);
    }

    public SkillManagementAdminOperationTracePage(
            int matchedRootEvents,
            int returnedRootEvents,
            int traceableRootEvents,
            int untraceableRootEvents,
            int filteredTraces,
            boolean truncated,
            int childEventLimit,
            String statusFilter,
            List<SkillManagementAdminOperationTrace> traces) {
        this(matchedRootEvents, returnedRootEvents, traceableRootEvents, untraceableRootEvents,
                filteredTraces, 0, truncated, childEventLimit, 0, 0, 0, 0, statusFilter, traces);
    }

    public SkillManagementAdminOperationTracePage {
        matchedRootEvents = SkillManagementAdminValueSupport.nonNegative(matchedRootEvents);
        returnedRootEvents = SkillManagementAdminValueSupport.nonNegative(returnedRootEvents);
        traceableRootEvents = SkillManagementAdminValueSupport.nonNegative(traceableRootEvents);
        untraceableRootEvents = SkillManagementAdminValueSupport.nonNegative(untraceableRootEvents);
        filteredTraces = SkillManagementAdminValueSupport.nonNegative(filteredTraces);
        childEventLimit = SkillManagementQueryLimits.normalize(childEventLimit);
        statusFilter = SkillManagementAdminValueSupport.identifier(statusFilter);
        traces = SkillManagementAdminValueSupport.nonNullList(traces);
        returnedTraces = traces.size();
        matchedRootEvents = SkillManagementAdminValueSupport.atLeast(matchedRootEvents, returnedRootEvents);
        returnedRootEvents = SkillManagementAdminValueSupport.atLeast(returnedRootEvents, returnedTraces);
        traceableRootEvents = SkillManagementAdminValueSupport.atLeast(traceableRootEvents, returnedTraces);
        filteredTraces = SkillManagementAdminValueSupport.atLeast(
                filteredTraces,
                traceableRootEvents - returnedTraces);
        truncated = truncated || matchedRootEvents > returnedRootEvents;
        healthyTraces = countStatus(traces, SkillManagementOperationTraceStatus.HEALTHY);
        failedTraces = countStatus(traces, SkillManagementOperationTraceStatus.FAILED);
        rootMissingTraces = countStatus(traces, SkillManagementOperationTraceStatus.ROOT_MISSING);
        missingOperationIdTraces = countStatus(traces, SkillManagementOperationTraceStatus.MISSING_OPERATION_ID);
    }

    private static int countStatus(
            List<SkillManagementAdminOperationTrace> traces,
            SkillManagementOperationTraceStatus status) {
        return SkillManagementAdminValueSupport.countMatching(
                traces,
                trace -> status.name().equals(trace.status()));
    }

    private static boolean inferredTruncated(
            int matchedRootEvents,
            List<SkillManagementAdminOperationTrace> traces) {
        return SkillManagementAdminValueSupport.nonNegative(matchedRootEvents)
                > SkillManagementAdminValueSupport.nonNullList(traces).size();
    }

    private static int traceCount(List<SkillManagementAdminOperationTrace> traces) {
        return SkillManagementAdminValueSupport.nonNullList(traces).size();
    }
}
