package tech.kayys.wayang.agent.api;

import java.util.List;
import java.util.Map;

/**
 * Typed REST payload for Hermes diagnostics inspections.
 */
public record HermesDiagnosticsResponse(
        String port,
        String operation,
        String target,
        boolean active,
        boolean dispatched,
        boolean successful,
        String status,
        String reason,
        Map<String, Object> metadata,
        String view,
        boolean ready,
        boolean runtimePortsReady,
        boolean skillPersistenceReady,
        boolean learningAuditConfigured,
        boolean learningAuditReady,
        List<String> attention,
        List<HermesOperationalAttention> attentionItems,
        HermesOperationalAttentionSummaryResponse attentionSummary,
        Map<String, Object> diagnostics,
        Map<String, Object> learningAuditRetentionObservation) {

    public HermesDiagnosticsResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        view = HermesResponseMetadata.text(view, "");
        attention = attention == null ? List.of() : List.copyOf(attention);
        attentionItems = attentionItems == null
                ? HermesOperationalAttention.fromMessages("runtime-diagnostics", "warning", 2, attention)
                : attentionItems.stream()
                        .filter(item -> item != null)
                        .toList();
        attentionSummary = attentionSummary == null
                ? HermesOperationalAttentionSummaryResponse.from(attentionItems)
                : attentionSummary;
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
        learningAuditRetentionObservation = learningAuditRetentionObservation == null
                ? Map.of()
                : Map.copyOf(learningAuditRetentionObservation);
    }

    public HermesDiagnosticsResponse(
            String port,
            String operation,
            String target,
            boolean active,
            boolean dispatched,
            boolean successful,
            String status,
            String reason,
            Map<String, Object> metadata,
            String view,
            boolean ready,
            boolean runtimePortsReady,
            boolean skillPersistenceReady,
            boolean learningAuditConfigured,
            boolean learningAuditReady,
            List<String> attention,
            Map<String, Object> diagnostics,
            Map<String, Object> learningAuditRetentionObservation) {
        this(
                port,
                operation,
                target,
                active,
                dispatched,
                successful,
                status,
                reason,
                metadata,
                view,
                ready,
                runtimePortsReady,
                skillPersistenceReady,
                learningAuditConfigured,
                learningAuditReady,
                attention,
                null,
                null,
                diagnostics,
                learningAuditRetentionObservation);
    }

    public static HermesDiagnosticsResponse from(HermesPortResponse response) {
        HermesPortResponse resolved = response == null ? HermesPortResponse.from(null) : response;
        Map<String, Object> metadata = resolved.metadata();
        return new HermesDiagnosticsResponse(
                resolved.port(),
                resolved.operation(),
                resolved.target(),
                resolved.active(),
                resolved.dispatched(),
                resolved.successful(),
                resolved.status(),
                resolved.reason(),
                metadata,
                HermesResponseMetadata.text(metadata.get("view"), ""),
                HermesResponseMetadata.bool(metadata.get("ready")),
                HermesResponseMetadata.bool(metadata.get("runtimePortsReady")),
                HermesResponseMetadata.bool(metadata.get("skillPersistenceReady")),
                HermesResponseMetadata.bool(metadata.get("learningAuditConfigured")),
                HermesResponseMetadata.bool(metadata.get("learningAuditReady")),
                HermesResponseMetadata.strings(metadata.get("attention")),
                null,
                null,
                HermesResponseMetadata.objectMap(metadata.get("diagnostics")),
                HermesResponseMetadata.learningAuditRetentionObservation(metadata));
    }
}
