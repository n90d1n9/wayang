package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesPortDispatchResult;

import java.util.Map;

/**
 * Stable REST payload for Hermes runtime port dispatches.
 */
public record HermesPortResponse(
        String port,
        String operation,
        String target,
        boolean active,
        boolean dispatched,
        boolean successful,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesPortResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static HermesPortResponse from(HermesPortDispatchResult result) {
        if (result == null) {
            return new HermesPortResponse(
                    "unknown",
                    "none",
                    "",
                    false,
                    false,
                    false,
                    "failed",
                    "Hermes port did not return a dispatch result",
                    Map.of());
        }
        return new HermesPortResponse(
                result.port(),
                result.operation(),
                result.target(),
                result.active(),
                result.dispatched(),
                result.successful(),
                result.status(),
                result.reason(),
                result.metadata());
    }
}
