package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

public record AgentRunEvent(
        String runId,
        long sequence,
        String type,
        AgentRunState state,
        String message,
        Map<String, Object> metadata) {

    public AgentRunEvent {
        runId = SdkText.trimToDefault(runId, "local-run");
        sequence = Math.max(1, sequence);
        state = state == null ? AgentRunState.UNKNOWN : state;
        type = SdkText.trimToDefault(type, typeFor(state));
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public static AgentRunEvent fromStatus(AgentRunStatus status, long sequence) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Run status is unknown.")
                : status;
        return new AgentRunEvent(
                normalized.handle().runId(),
                sequence,
                typeFor(normalized.handle().state()),
                normalized.handle().state(),
                normalized.message(),
                normalized.metadata());
    }

    public static String typeFor(AgentRunState state) {
        return "run." + AgentRunStates.wireName(state);
    }
}
