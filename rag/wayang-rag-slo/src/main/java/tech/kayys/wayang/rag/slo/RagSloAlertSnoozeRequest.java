package tech.kayys.wayang.rag.slo;

public record RagSloAlertSnoozeRequest(
        Long durationMs,
        String scope) {
}
