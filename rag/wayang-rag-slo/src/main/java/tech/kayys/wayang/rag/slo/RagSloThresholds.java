package tech.kayys.wayang.rag.slo;

import java.util.Map;

public record RagSloThresholds(
        double embeddingLatencyP95Ms,
        double searchLatencyP95Ms,
        double ingestLatencyP95Ms,
        double embeddingFailureRate,
        double searchFailureRate,
        long indexLagMs,
        double compactionFailureRate,
        long compactionCycleStalenessMs,
        double severityWarningMultiplier,
        double severityCriticalMultiplier,
        Map<String, Double> severityWarningByMetric,
        Map<String, Double> severityCriticalByMetric,
        boolean alertEnabled,
        String alertMinSeverity,
        long alertCooldownMs) {
}
