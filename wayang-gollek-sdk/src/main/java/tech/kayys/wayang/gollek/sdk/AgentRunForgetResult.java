package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

public record AgentRunForgetResult(
        String runId,
        boolean forgotten,
        String outcome,
        String message,
        Map<String, Object> metadata) {

    public AgentRunForgetResult(
            String runId,
            boolean forgotten,
            String message,
            Map<String, Object> metadata) {
        this(runId, forgotten, null, message, metadata);
    }

    public AgentRunForgetResult {
        runId = SdkText.trimToEmpty(runId);
        outcome = AgentRunOutcomes.normalizeOrDefault(
                outcome,
                forgotten ? AgentRunOutcomes.FORGOTTEN : AgentRunOutcomes.NOT_FOUND);
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public static AgentRunForgetResult forgotten(AgentRunStatus status) {
        AgentRunStatus normalized = status == null
                ? AgentRunStatus.unknown("", "Run status was forgotten.")
                : status;
        return new AgentRunForgetResult(
                normalized.handle().runId(),
                true,
                AgentRunOutcomes.FORGOTTEN,
                "Forgot Wayang run status.",
                Map.of(
                        "state", normalized.handle().state().name(),
                        "strategy", normalized.handle().strategy()));
    }

    public static AgentRunForgetResult notFound(String runId, String message) {
        return new AgentRunForgetResult(
                runId,
                false,
                AgentRunOutcomes.NOT_FOUND,
                SdkText.trimToDefault(message, "No run status is recorded for this run id."),
                Map.of());
    }
}
