package tech.kayys.wayang.hitl.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Configuration for notifications sent for a HITL task.
 */
public class NotificationConfig {

    @JsonProperty("email")
    @JsonPropertyDescription("Whether to send email notifications. Defaults to true.")
    private Boolean email = true;

    @JsonProperty("slack")
    @JsonPropertyDescription("Whether to send Slack notifications. Defaults to false.")
    private Boolean slack = false;

    @JsonProperty("webhookUrl")
    @JsonPropertyDescription("Optional webhook URL to call when the task is assigned or completed.")
    private String webhookUrl;

    public NotificationConfig() {
    }

    public Boolean getEmail() {
        return email;
    }

    public void setEmail(Boolean email) {
        this.email = email;
    }

    public Boolean getSlack() {
        return slack;
    }

    public void setSlack(Boolean slack) {
        this.slack = slack;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
