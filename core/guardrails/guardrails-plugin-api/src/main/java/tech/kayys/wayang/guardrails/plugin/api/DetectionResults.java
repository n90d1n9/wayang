package tech.kayys.wayang.guardrails.plugin.api;

import java.util.List;

/**
 * Collection of detection results.
 */
public record DetectionResults(List<DetectionResult> results) {
    public boolean isSafe() {
        return results.stream().allMatch(DetectionResult::safe);
    }

    public boolean hasBlockingIssues() {
        return !isSafe();
    }
}
