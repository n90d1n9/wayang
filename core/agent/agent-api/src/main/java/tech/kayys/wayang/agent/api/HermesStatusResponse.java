package tech.kayys.wayang.agent.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Compact operational status payload for Hermes mode.
 */
public record HermesStatusResponse(
        String status,
        boolean ready,
        boolean diagnosticsConfigured,
        boolean diagnosticsReady,
        boolean journalConfigured,
        boolean learningAuditConfigured,
        boolean learningAuditReady,
        Map<String, Object> diagnostics,
        List<String> attention,
        List<HermesOperationalAttention> attentionItems,
        HermesOperationalAttentionSummaryResponse attentionSummary,
        Map<String, Object> learningAuditRetentionObservation) {

    public static final String STATUS_UP = "UP";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    public HermesStatusResponse {
        status = clean(status, STATUS_UNAVAILABLE);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
        attention = attention == null ? List.of() : List.copyOf(attention);
        attentionItems = attentionItems == null
                ? HermesOperationalAttention.fromMessages("hermes-status", "warning", 2, attention)
                : attentionItems.stream()
                        .filter(item -> item != null)
                        .toList();
        attentionSummary = attentionSummary == null
                ? HermesOperationalAttentionSummaryResponse.from(attentionItems)
                : attentionSummary;
        learningAuditRetentionObservation = learningAuditRetentionObservation == null
                ? Map.of()
                : Map.copyOf(learningAuditRetentionObservation);
    }

    public HermesStatusResponse(
            String status,
            boolean ready,
            boolean diagnosticsConfigured,
            boolean diagnosticsReady,
            boolean journalConfigured,
            boolean learningAuditConfigured,
            boolean learningAuditReady,
            Map<String, Object> diagnostics,
            List<String> attention,
            Map<String, Object> learningAuditRetentionObservation) {
        this(
                status,
                ready,
                diagnosticsConfigured,
                diagnosticsReady,
                journalConfigured,
                learningAuditConfigured,
                learningAuditReady,
                diagnostics,
                attention,
                null,
                null,
                learningAuditRetentionObservation);
    }

    public static HermesStatusResponse from(
            HermesPortResponse diagnosticsResponse,
            boolean diagnosticsConfigured,
            boolean journalConfigured,
            boolean learningAuditConfigured,
            boolean learningAuditReady) {
        Map<String, Object> metadata = diagnosticsResponse == null
                ? Map.of()
                : diagnosticsResponse.metadata();
        boolean diagnosticsReady = diagnosticsConfigured
                && diagnosticsResponse != null
                && diagnosticsResponse.successful()
                && booleanValue(metadata.get("ready"));
        List<String> attention = new ArrayList<>(attentionFrom(metadata.get("attention")));
        if (!journalConfigured) {
            attention.add(HermesOperationalMessages.MISSING_JOURNAL_PORT);
        }
        if (!learningAuditConfigured) {
            attention.add(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT);
        }
        boolean ready = diagnosticsReady && journalConfigured && learningAuditReady;
        return new HermesStatusResponse(
                statusFor(diagnosticsConfigured, ready),
                ready,
                diagnosticsConfigured,
                diagnosticsReady,
                journalConfigured,
                learningAuditConfigured,
                learningAuditReady,
                metadata,
                attention,
                null,
                null,
                HermesResponseMetadata.learningAuditRetentionObservation(metadata));
    }

    public static HermesStatusResponse unavailable(
            boolean journalConfigured,
            boolean learningAuditConfigured,
            boolean learningAuditReady,
            String reason) {
        List<String> attention = new ArrayList<>();
        String message = clean(reason, "");
        if (!message.isEmpty()) {
            attention.add(message);
        }
        if (!journalConfigured) {
            attention.add(HermesOperationalMessages.MISSING_JOURNAL_PORT);
        }
        if (!learningAuditConfigured) {
            attention.add(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT);
        }
        return new HermesStatusResponse(
                STATUS_UNAVAILABLE,
                false,
                false,
                false,
                journalConfigured,
                learningAuditConfigured,
                learningAuditReady,
                Map.of(),
                attention,
                null,
                null,
                Map.of());
    }

    private static String statusFor(boolean diagnosticsConfigured, boolean ready) {
        if (ready) {
            return STATUS_UP;
        }
        return diagnosticsConfigured ? STATUS_DEGRADED : STATUS_UNAVAILABLE;
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static List<String> attentionFrom(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(message -> !message.isEmpty())
                .toList();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
