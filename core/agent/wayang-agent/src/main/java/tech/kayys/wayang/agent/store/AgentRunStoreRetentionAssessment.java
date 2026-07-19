package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

/**
 * Diagnostic summary of how a retention policy affects one run-store snapshot.
 */
public record AgentRunStoreRetentionAssessment(
        AgentRunStoreRetentionPolicy policy,
        int totalRuns,
        int retainedRuns,
        int prunedRuns,
        int totalStatuses,
        int retainedStatuses,
        int prunedStatuses,
        int totalEvents,
        int retainedEvents,
        int prunedEvents,
        List<String> retainedRunIds,
        List<String> prunedRunIds,
        Map<String, Integer> prunedEventsByRun) {

    public AgentRunStoreRetentionAssessment {
        policy = policy == null ? AgentRunStoreRetentionPolicy.defaults() : policy;
        totalRuns = Math.max(0, totalRuns);
        retainedRuns = Math.max(0, retainedRuns);
        prunedRuns = Math.max(0, prunedRuns);
        totalStatuses = Math.max(0, totalStatuses);
        retainedStatuses = Math.max(0, retainedStatuses);
        prunedStatuses = Math.max(0, prunedStatuses);
        totalEvents = Math.max(0, totalEvents);
        retainedEvents = Math.max(0, retainedEvents);
        prunedEvents = Math.max(0, prunedEvents);
        retainedRunIds = retainedRunIds == null ? List.of() : List.copyOf(retainedRunIds);
        prunedRunIds = prunedRunIds == null ? List.of() : List.copyOf(prunedRunIds);
        prunedEventsByRun = prunedEventsByRun == null ? Map.of() : SdkMaps.orderedTypedCopy(prunedEventsByRun);
    }

    public boolean pruned() {
        return prunedRuns > 0 || prunedStatuses > 0 || prunedEvents > 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("policy", policy.toMap());
        values.put("pruned", pruned());
        values.put("totalRuns", totalRuns);
        values.put("retainedRuns", retainedRuns);
        values.put("prunedRuns", prunedRuns);
        values.put("totalStatuses", totalStatuses);
        values.put("retainedStatuses", retainedStatuses);
        values.put("prunedStatuses", prunedStatuses);
        values.put("totalEvents", totalEvents);
        values.put("retainedEvents", retainedEvents);
        values.put("prunedEvents", prunedEvents);
        values.put("retainedRunIds", retainedRunIds);
        values.put("prunedRunIds", prunedRunIds);
        values.put("prunedEventsByRun", prunedEventsByRun);
        return SdkMaps.copy(values);
    }
}
