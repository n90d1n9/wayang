package tech.kayys.wayang.hitl.domain;

public enum HumanTaskStatus {
    CREATED,
    ASSIGNED,
    IN_PROGRESS,
    ESCALATED,
    COMPLETED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }
}