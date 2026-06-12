package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Policy decision for one Hermes learning-loop candidate.
 */
public record HermesLearningAssessment(
        boolean eligible,
        boolean forced,
        String reason,
        double qualityScore,
        double qualityThreshold,
        Map<String, Object> metrics) {

    public HermesLearningAssessment {
        reason = reason == null || reason.isBlank() ? "unspecified" : reason.trim();
        qualityScore = clamp(qualityScore);
        qualityThreshold = clamp(qualityThreshold <= 0.0
                ? HermesLearningQualityProfile.DEFAULT_THRESHOLD
                : qualityThreshold);
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    }

    public HermesLearningAssessment(boolean eligible, boolean forced, String reason) {
        this(
                eligible,
                forced,
                reason,
                0.0,
                HermesLearningQualityProfile.DEFAULT_THRESHOLD,
                Map.of());
    }

    public static HermesLearningAssessment eligible(String reason) {
        return eligible(reason, false);
    }

    public static HermesLearningAssessment eligible(String reason, boolean forced) {
        return new HermesLearningAssessment(true, forced, reason);
    }

    public static HermesLearningAssessment eligible(
            String reason,
            boolean forced,
            HermesLearningQualityProfile quality) {
        return from(true, forced, reason, quality);
    }

    public static HermesLearningAssessment skipped(String reason) {
        return new HermesLearningAssessment(false, false, reason);
    }

    public static HermesLearningAssessment skipped(
            String reason,
            HermesLearningQualityProfile quality) {
        return from(false, false, reason, quality);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("eligible", eligible);
        metadata.put("forced", forced);
        metadata.put("reason", reason);
        metadata.put("qualityScore", String.format(java.util.Locale.ROOT, "%.2f", qualityScore));
        metadata.put("qualityThreshold", String.format(java.util.Locale.ROOT, "%.2f", qualityThreshold));
        metadata.put("metrics", metrics);
        return Map.copyOf(metadata);
    }

    private static HermesLearningAssessment from(
            boolean eligible,
            boolean forced,
            String reason,
            HermesLearningQualityProfile quality) {
        HermesLearningQualityProfile resolved = quality == null
                ? HermesLearningQualityProfile.from(null, null)
                : quality;
        return new HermesLearningAssessment(
                eligible,
                forced,
                reason,
                resolved.qualityScore(),
                resolved.threshold(),
                resolved.toMetadata());
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
