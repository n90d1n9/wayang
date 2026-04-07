package tech.kayys.wayang.rag.slo;

import java.util.Map;

public record RagSloConfigUpdate(
        Double embeddingLatencyP95Ms,
        Double searchLatencyP95Ms,
        Double ingestLatencyP95Ms,
        Double embeddingFailureRate,
        Double searchFailureRate,
        Long indexLagMs,
        Double compactionFailureRate,
        Long compactionCycleStalenessMs,
        Double severityWarningMultiplier,
        Double severityCriticalMultiplier,
        Map<String, Double> severityWarningByMetric,
        Map<String, Double> severityCriticalByMetric,
        Boolean alertEnabled,
        String alertMinSeverity,
        Long alertCooldownMs) {
}
