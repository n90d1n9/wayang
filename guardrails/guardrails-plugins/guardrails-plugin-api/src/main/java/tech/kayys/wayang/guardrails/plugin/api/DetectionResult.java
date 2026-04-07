package tech.kayys.wayang.guardrails.plugin.api;

import java.util.List;
import java.util.Collections;

/**
 * Result of a detection check.
 */
public record DetectionResult(
        String detectorId,
        String category,
        boolean safe,
        String message,
        List<Finding> findings) {

    public static DetectionResult safe(String detectorId, String category) {
        return new DetectionResult(detectorId, category, true, null, Collections.emptyList());
    }

    public static DetectionResult warning(String detectorId, String category, String message, List<Finding> findings) {
        return new DetectionResult(detectorId, category, true, message, findings);
    }

    public static DetectionResult blocked(String detectorId, String category, String message) {
        return new DetectionResult(detectorId, category, false, message, Collections.emptyList());
    }

    public static DetectionResult blocked(String detectorId, String category, String message, List<Finding> findings) {
        return new DetectionResult(detectorId, category, false, message, findings);
    }
}
