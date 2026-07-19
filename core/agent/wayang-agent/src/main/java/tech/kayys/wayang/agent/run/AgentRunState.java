package tech.kayys.wayang.agent.run;

public enum AgentRunState {
    UNKNOWN,
    CREATED,
    PLANNED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
