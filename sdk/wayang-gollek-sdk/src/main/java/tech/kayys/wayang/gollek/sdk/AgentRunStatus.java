package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

public record AgentRunStatus(
        AgentRunHandle handle,
        boolean known,
        String message,
        Map<String, Object> metadata) {

    public AgentRunStatus {
        handle = handle == null ? AgentRunHandle.unknown("") : handle;
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public static AgentRunStatus unknown(String runId, String message) {
        return new AgentRunStatus(
                AgentRunHandle.unknown(runId),
                false,
                SdkText.trimToDefault(message, "Run status is unknown."),
                Map.of());
    }

    public String outcome() {
        if (!known) {
            return AgentRunOutcomes.UNKNOWN;
        }
        return handle.terminal() ? AgentRunOutcomes.TERMINAL : AgentRunOutcomes.PENDING;
    }
}
