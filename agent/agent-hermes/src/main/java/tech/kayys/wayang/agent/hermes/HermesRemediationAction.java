package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single machine-readable remediation action derived from dispatch attention.
 */
public record HermesRemediationAction(
        String port,
        String operation,
        String target,
        String status,
        String action,
        String severity,
        boolean retryable,
        String reason,
        Map<String, Object> metadata) {

    public HermesRemediationAction {
        port = HermesDirectiveSupport.clean(port, "unknown");
        operation = HermesDirectiveSupport.clean(operation, "none");
        target = HermesDirectiveSupport.clean(target, "");
        status = HermesDirectiveSupport.clean(status, "unknown");
        action = HermesDirectiveSupport.clean(action, "manual-review");
        severity = HermesDirectiveSupport.clean(severity, "warning");
        reason = HermesDirectiveSupport.clean(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesRemediationAction from(HermesDirectiveDispatchAttention attention) {
        return new HermesRemediationAction(
                attention.port(),
                attention.operation(),
                attention.target(),
                attention.status(),
                attention.recommendedAction(),
                attention.severity(),
                attention.retryable(),
                attention.reason(),
                attention.metadata());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("port", port);
        values.put("operation", operation);
        values.put("target", target);
        values.put("status", status);
        values.put("action", action);
        values.put("severity", severity);
        values.put("retryable", retryable);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}
