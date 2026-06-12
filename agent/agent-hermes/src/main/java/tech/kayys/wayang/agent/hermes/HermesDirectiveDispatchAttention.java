package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actionable pointer for an active directive that did not complete successfully.
 */
public record HermesDirectiveDispatchAttention(
        String port,
        String operation,
        String target,
        String status,
        String reason,
        String severity,
        String recommendedAction,
        boolean retryable,
        Map<String, Object> metadata) {

    public HermesDirectiveDispatchAttention {
        port = HermesDirectiveSupport.clean(port, "unknown");
        operation = HermesDirectiveSupport.clean(operation, "none");
        target = HermesDirectiveSupport.clean(target, "");
        status = HermesDirectiveSupport.clean(status, "unknown");
        reason = HermesDirectiveSupport.clean(reason, "");
        severity = HermesDirectiveSupport.clean(severity, "warning");
        recommendedAction = HermesDirectiveSupport.clean(recommendedAction, "review-dispatch-result");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesDirectiveDispatchAttention from(HermesPortDispatchResult result) {
        return new HermesDirectiveDispatchAttention(
                result.port(),
                result.operation(),
                result.target(),
                result.status(),
                result.reason(),
                severityFor(result),
                recommendedActionFor(result),
                retryableFor(result),
                result.metadata());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("port", port);
        values.put("operation", operation);
        values.put("target", target);
        values.put("status", status);
        values.put("reason", reason);
        values.put("severity", severity);
        values.put("recommendedAction", recommendedAction);
        values.put("retryable", retryable);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static String severityFor(HermesPortDispatchResult result) {
        if (hasStatus(result, "unavailable")) {
            return "critical";
        }
        if (hasStatus(result, "failed")) {
            return "warning";
        }
        return "info";
    }

    private static String recommendedActionFor(HermesPortDispatchResult result) {
        if (hasStatus(result, "unavailable")) {
            Map<String, Object> runtimePort = runtimePort(result);
            if (Boolean.FALSE.equals(runtimePort.get("configured")) || Boolean.TRUE.equals(runtimePort.get("noop"))) {
                return "configure-runtime-port";
            }
            return "restore-runtime-port";
        }
        if (hasStatus(result, "failed")) {
            return "inspect-runtime-adapter";
        }
        return "review-dispatch-result";
    }

    private static boolean retryableFor(HermesPortDispatchResult result) {
        return hasStatus(result, "failed");
    }

    private static boolean hasStatus(HermesPortDispatchResult result, String status) {
        return result != null && result.status().equalsIgnoreCase(status);
    }

    private static Map<String, Object> runtimePort(HermesPortDispatchResult result) {
        Object value = result == null ? null : result.metadata().get("runtimePort");
        if (value instanceof Map<?, ?> values) {
            Map<String, Object> runtimePort = new LinkedHashMap<>();
            values.forEach((key, portValue) -> runtimePort.put(String.valueOf(key), portValue));
            return runtimePort;
        }
        return Map.of();
    }
}
