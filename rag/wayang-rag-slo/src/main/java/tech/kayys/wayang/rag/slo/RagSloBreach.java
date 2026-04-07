package tech.kayys.wayang.rag.slo;

public record RagSloBreach(
        String metric,
        double observed,
        double threshold,
        String severity,
        String message) {
}
