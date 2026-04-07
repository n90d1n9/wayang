package tech.kayys.wayang.hitl.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Map;

/**
 * Strongly-typed configuration DTO for Human-in-the-Loop (HITL) task nodes.
 */
public class HumanTaskConfig {

    @JsonProperty("assignTo")
    @JsonPropertyDescription("User ID, group ID, or role to assign the task to.")
    private String assignTo;

    @JsonProperty("assigneeType")
    @JsonPropertyDescription("How to interpret assignTo: USER, GROUP, or ROLE. Defaults to USER.")
    private String assigneeType = "USER";

    @JsonProperty("taskType")
    @JsonPropertyDescription("Type of human task: approval, review, data_entry, etc. Defaults to approval.")
    private String taskType = "approval";

    @JsonProperty("title")
    @JsonPropertyDescription("Short title displayed to the assignee.")
    private String title;

    @JsonProperty("description")
    @JsonPropertyDescription("Detailed task description shown to the assignee.")
    private String description;

    @JsonProperty("priority")
    @JsonPropertyDescription("Priority level from 1 (lowest) to 5 (highest). Defaults to 3.")
    private Integer priority = 3;

    @JsonProperty("dueInHours")
    @JsonPropertyDescription("Number of hours from now until the task is due.")
    private Integer dueInHours;

    @JsonProperty("dueInDays")
    @JsonPropertyDescription("Number of days from now until the task is due.")
    private Integer dueInDays;

    @JsonProperty("tenantId")
    @JsonPropertyDescription("Tenant identifier for multi-tenant deployments. Defaults to 'default-tenant'.")
    private String tenantId = "default-tenant";

    @JsonProperty("formSchema")
    @JsonPropertyDescription("Optional JSON schema defining the form fields the user must fill in.")
    private Map<String, Object> formSchema;

    @JsonProperty("formData")
    @JsonPropertyDescription("Optional pre-populated form data.")
    private Map<String, Object> formData;

    @JsonProperty("escalationConfig")
    @JsonPropertyDescription("Optional escalation rules if the task is not completed on time.")
    private EscalationConfig escalationConfig;

    @JsonProperty("notificationConfig")
    @JsonPropertyDescription("Notification preferences for this task.")
    private NotificationConfig notificationConfig;

    public HumanTaskConfig() {
    }

    public String getAssignTo() {
        return assignTo;
    }

    public void setAssignTo(String assignTo) {
        this.assignTo = assignTo;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public void setAssigneeType(String assigneeType) {
        this.assigneeType = assigneeType;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getDueInHours() {
        return dueInHours;
    }

    public void setDueInHours(Integer dueInHours) {
        this.dueInHours = dueInHours;
    }

    public Integer getDueInDays() {
        return dueInDays;
    }

    public void setDueInDays(Integer dueInDays) {
        this.dueInDays = dueInDays;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(Map<String, Object> formSchema) {
        this.formSchema = formSchema;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public EscalationConfig getEscalationConfig() {
        return escalationConfig;
    }

    public void setEscalationConfig(EscalationConfig escalationConfig) {
        this.escalationConfig = escalationConfig;
    }

    public NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public void setNotificationConfig(NotificationConfig notificationConfig) {
        this.notificationConfig = notificationConfig;
    }
}
