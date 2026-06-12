package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate result for a Hermes request-plan directive dispatch pass.
 */
public record HermesDirectiveDispatchReport(
        List<HermesPortDispatchResult> results,
        Map<String, Object> runtimePorts) {

    public HermesDirectiveDispatchReport(List<HermesPortDispatchResult> results) {
        this(results, Map.of());
    }

    public HermesDirectiveDispatchReport {
        results = results == null ? List.of() : List.copyOf(results);
        runtimePorts = runtimePorts == null ? Map.of() : Map.copyOf(runtimePorts);
    }

    public HermesDirectiveDispatchSummary summary() {
        return HermesDirectiveDispatchSummary.from(results);
    }

    public boolean successful() {
        return summary().successful();
    }

    public String outcome() {
        return summary().outcome();
    }

    public long totalCount() {
        return summary().totalCount();
    }

    public long activeCount() {
        return summary().activeCount();
    }

    public long dispatchedCount() {
        return summary().dispatchedCount();
    }

    public long skippedCount() {
        return summary().skippedCount();
    }

    public long unavailableCount() {
        return summary().unavailableCount();
    }

    public long failedCount() {
        return summary().failedCount();
    }

    public long unsuccessfulCount() {
        return summary().unsuccessfulCount();
    }

    public int attentionCount() {
        return summary().attentionCount();
    }

    public List<HermesDirectiveDispatchAttention> attention() {
        return summary().attention();
    }

    public HermesRemediationPlan remediationPlan() {
        return summary().remediationPlan();
    }

    public Map<String, Object> toMetadata() {
        HermesDirectiveDispatchSummary summary = summary();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("successful", summary.successful());
        metadata.put("outcome", summary.outcome());
        metadata.put("totalCount", summary.totalCount());
        metadata.put("activeCount", summary.activeCount());
        metadata.put("dispatchedCount", summary.dispatchedCount());
        metadata.put("skippedCount", summary.skippedCount());
        metadata.put("unavailableCount", summary.unavailableCount());
        metadata.put("failedCount", summary.failedCount());
        metadata.put("unsuccessfulCount", summary.unsuccessfulCount());
        metadata.put("attentionCount", summary.attentionCount());
        metadata.put("attention", summary.attention().stream()
                .map(HermesDirectiveDispatchAttention::toMetadata)
                .toList());
        metadata.put("remediationPlan", summary.remediationPlan().toMetadata());
        metadata.put("statusCounts", summary.statusCounts());
        metadata.put("summary", summary.toMetadata());
        metadata.put("runtimePorts", runtimePorts);
        metadata.put("results", results.stream()
                .map(HermesPortDispatchResult::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }
}
