package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result returned by a Hermes runtime port after a directive handoff.
 */
public record HermesPortDispatchResult(
        String port,
        String operation,
        String target,
        boolean active,
        boolean dispatched,
        boolean successful,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesPortDispatchResult {
        port = HermesDirectiveSupport.clean(port, "unknown");
        operation = HermesDirectiveSupport.clean(operation, "none");
        target = HermesDirectiveSupport.clean(target, "");
        status = HermesDirectiveSupport.clean(status, successful ? "ok" : "failed");
        reason = HermesDirectiveSupport.clean(reason, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesPortDispatchResult skipped(
            String port,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata) {
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                false,
                false,
                true,
                "skipped",
                reason,
                metadata);
    }

    public static HermesPortDispatchResult noop(
            String port,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata) {
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                true,
                true,
                "noop",
                reason,
                metadata);
    }

    public static HermesPortDispatchResult failed(
            String port,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata) {
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                true,
                false,
                "failed",
                reason,
                metadata);
    }

    public static HermesPortDispatchResult unavailable(
            String port,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata) {
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                false,
                false,
                "unavailable",
                reason,
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("port", port);
        values.put("operation", operation);
        values.put("target", target);
        values.put("active", active);
        values.put("dispatched", dispatched);
        values.put("successful", successful);
        values.put("status", status);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}
