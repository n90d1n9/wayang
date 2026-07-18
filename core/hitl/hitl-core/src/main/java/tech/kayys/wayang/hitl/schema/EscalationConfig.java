package tech.kayys.wayang.hitl.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Configuration for task escalation within a HITL task.
 */
public class EscalationConfig {

    @JsonProperty("escalateTo")
    @JsonPropertyDescription("User ID, group, or role to escalate the task to if not completed in time.")
    private String escalateTo;

    @JsonProperty("escalateAfterHours")
    @JsonPropertyDescription("Hours to wait before escalating. Defaults to 24 hours.")
    private Integer escalateAfterHours = 24;

    @JsonProperty("notifyOnEscalation")
    @JsonPropertyDescription("Whether to send a notification when the task is escalated.")
    private Boolean notifyOnEscalation = true;

    public EscalationConfig() {
    }

    public String getEscalateTo() {
        return escalateTo;
    }

    public void setEscalateTo(String escalateTo) {
        this.escalateTo = escalateTo;
    }

    public Integer getEscalateAfterHours() {
        return escalateAfterHours;
    }

    public void setEscalateAfterHours(Integer escalateAfterHours) {
        this.escalateAfterHours = escalateAfterHours;
    }

    public Boolean getNotifyOnEscalation() {
        return notifyOnEscalation;
    }

    public void setNotifyOnEscalation(Boolean notifyOnEscalation) {
        this.notifyOnEscalation = notifyOnEscalation;
    }
}
