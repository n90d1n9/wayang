package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact operational summary for a directive dispatch pass.
 */
public record HermesDirectiveDispatchSummary(
        long totalCount,
        long activeCount,
        long dispatchedCount,
        long skippedCount,
        long unavailableCount,
        long failedCount,
        long unsuccessfulCount,
        Map<String, Long> statusCounts,
        List<HermesDirectiveDispatchAttention> attention,
        HermesRemediationPlan remediationPlan) {

    public HermesDirectiveDispatchSummary {
        statusCounts = statusCounts == null ? Map.of() : Map.copyOf(statusCounts);
        attention = attention == null ? List.of() : List.copyOf(attention);
        remediationPlan = remediationPlan == null ? HermesRemediationPlan.from(attention) : remediationPlan;
    }

    public static HermesDirectiveDispatchSummary from(List<HermesPortDispatchResult> results) {
        List<HermesPortDispatchResult> values = HermesCollections.copyNonNull(results);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (HermesPortDispatchResult result : values) {
            statusCounts.merge(HermesDirectiveSupport.clean(result.status(), "unknown"), 1L, Long::sum);
        }
        List<HermesDirectiveDispatchAttention> attention = values.stream()
                .filter(result -> result.active() && !result.successful())
                .map(HermesDirectiveDispatchAttention::from)
                .toList();
        return new HermesDirectiveDispatchSummary(
                values.size(),
                values.stream().filter(HermesPortDispatchResult::active).count(),
                values.stream().filter(HermesPortDispatchResult::dispatched).count(),
                values.stream().filter(result -> hasStatus(result, "skipped")).count(),
                values.stream().filter(result -> hasStatus(result, "unavailable")).count(),
                values.stream().filter(result -> hasStatus(result, "failed")).count(),
                values.stream().filter(result -> !result.successful()).count(),
                statusCounts,
                attention,
                HermesRemediationPlan.from(attention));
    }

    public boolean successful() {
        return unsuccessfulCount == 0;
    }

    public int attentionCount() {
        return attention.size();
    }

    public String outcome() {
        if (activeCount == 0) {
            return "idle";
        }
        if (successful()) {
            return "healthy";
        }
        if (activeCount == unsuccessfulCount) {
            return "blocked";
        }
        return "degraded";
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("successful", successful());
        metadata.put("outcome", outcome());
        metadata.put("totalCount", totalCount);
        metadata.put("activeCount", activeCount);
        metadata.put("dispatchedCount", dispatchedCount);
        metadata.put("skippedCount", skippedCount);
        metadata.put("unavailableCount", unavailableCount);
        metadata.put("failedCount", failedCount);
        metadata.put("unsuccessfulCount", unsuccessfulCount);
        metadata.put("attentionCount", attentionCount());
        metadata.put("attention", attention.stream()
                .map(HermesDirectiveDispatchAttention::toMetadata)
                .toList());
        metadata.put("remediationPlan", remediationPlan.toMetadata());
        metadata.put("statusCounts", statusCounts);
        return Map.copyOf(metadata);
    }

    private static boolean hasStatus(HermesPortDispatchResult result, String status) {
        return result.status().equalsIgnoreCase(status);
    }
}
