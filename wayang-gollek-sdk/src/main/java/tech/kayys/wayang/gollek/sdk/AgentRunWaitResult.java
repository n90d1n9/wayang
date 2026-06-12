package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

public record AgentRunWaitResult(
        String runId,
        AgentRunStatus status,
        boolean terminal,
        boolean timedOut,
        int attempts,
        long elapsedMillis,
        String message,
        Map<String, Object> metadata) {

    public AgentRunWaitResult {
        status = status == null ? AgentRunStatus.unknown(runId, "Run status is unknown.") : status;
        runId = SdkText.trimToDefault(runId, status.handle().runId());
        terminal = status.handle().terminal();
        attempts = Math.max(0, attempts);
        elapsedMillis = Math.max(0, elapsedMillis);
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public String outcome() {
        if (terminal) {
            return AgentRunOutcomes.TERMINAL;
        }
        if (timedOut) {
            return AgentRunOutcomes.TIMEOUT;
        }
        return status.known() ? AgentRunOutcomes.PENDING : AgentRunOutcomes.UNKNOWN;
    }
}
