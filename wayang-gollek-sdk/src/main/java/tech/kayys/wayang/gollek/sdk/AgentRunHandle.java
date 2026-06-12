package tech.kayys.wayang.gollek.sdk;

public record AgentRunHandle(
        String runId,
        AgentRunState state,
        String strategy) {

    public AgentRunHandle {
        runId = SdkText.trimToDefault(runId, "local-run");
        state = state == null ? AgentRunState.CREATED : state;
        strategy = SdkText.trimToDefault(strategy, "reactive-agent");
    }

    public static AgentRunHandle completed(String runId, String strategy) {
        return new AgentRunHandle(runId, AgentRunState.COMPLETED, strategy);
    }

    public static AgentRunHandle failed(String runId, String strategy) {
        return new AgentRunHandle(runId, AgentRunState.FAILED, strategy);
    }

    public static AgentRunHandle unknown(String runId) {
        return new AgentRunHandle(runId, AgentRunState.UNKNOWN, "unknown");
    }

    public boolean terminal() {
        return state.terminal();
    }
}
