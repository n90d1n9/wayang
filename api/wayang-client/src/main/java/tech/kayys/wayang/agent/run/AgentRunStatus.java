package tech.kayys.wayang.agent.run;

import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

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
