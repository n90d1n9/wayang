package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

public record AgentRunCancelResult(
        String runId,
        boolean cancelled,
        String outcome,
        AgentRunHandle handle,
        String message,
        Map<String, Object> metadata) {

    public AgentRunCancelResult(
            String runId,
            boolean cancelled,
            AgentRunHandle handle,
            String message,
            Map<String, Object> metadata) {
        this(runId, cancelled, null, handle, message, metadata);
    }

    public AgentRunCancelResult {
        handle = handle == null ? AgentRunHandle.unknown(runId) : handle;
        runId = SdkText.trimToDefault(runId, handle.runId());
        outcome = AgentRunOutcomes.normalizeOrDefault(outcome, inferOutcome(cancelled, handle));
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public static AgentRunCancelResult cancelled(AgentRunStatus status) {
        AgentRunStatus normalized = status == null
                ? new AgentRunStatus(
                        new AgentRunHandle("", AgentRunState.CANCELLED, "unknown"),
                        true,
                        "Run was cancelled.",
                        Map.of())
                : status;
        return new AgentRunCancelResult(
                normalized.handle().runId(),
                true,
                AgentRunOutcomes.CANCELLED,
                normalized.handle(),
                SdkText.trimToDefault(normalized.message(), "Run was cancelled."),
                normalized.metadata());
    }

    public static AgentRunCancelResult notCancellable(AgentRunStatus status, String message) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Run status is unknown.")
                : status;
        return new AgentRunCancelResult(
                normalized.handle().runId(),
                false,
                AgentRunOutcomes.NOT_CANCELLABLE,
                normalized.handle(),
                SdkText.trimToDefault(message, "Run cannot be cancelled."),
                Map.of(
                        "state", normalized.handle().state().name(),
                        "strategy", normalized.handle().strategy()));
    }

    public static AgentRunCancelResult notCancellable(String runId, String message) {
        return new AgentRunCancelResult(
                runId,
                false,
                AgentRunOutcomes.NOT_CANCELLABLE,
                AgentRunHandle.unknown(runId),
                SdkText.trimToDefault(message, "Run cannot be cancelled."),
                Map.of());
    }

    public static AgentRunCancelResult notFound(String runId, String message) {
        return new AgentRunCancelResult(
                runId,
                false,
                AgentRunOutcomes.NOT_FOUND,
                AgentRunHandle.unknown(runId),
                SdkText.trimToDefault(message, "No run status is recorded for this run id."),
                Map.of());
    }

    private static String inferOutcome(boolean cancelled, AgentRunHandle handle) {
        if (cancelled) {
            return AgentRunOutcomes.CANCELLED;
        }
        AgentRunHandle normalized = handle == null ? AgentRunHandle.unknown("") : handle;
        return normalized.state() == AgentRunState.UNKNOWN
                ? AgentRunOutcomes.UNKNOWN
                : AgentRunOutcomes.NOT_CANCELLABLE;
    }
}
