package tech.kayys.wayang.agent.integration.gamelan.graph;

/**
 * Definition of a workflow step for graph storage.
 * Includes step identity, ordering, and type information.
 */
public class WorkflowStepDefinition {

    private String stepId;
    private String name;
    private Integer order;
    private String type;
    private String description;

    /**
     * Creates a workflow step definition.
     */
    public WorkflowStepDefinition() {
    }

    /**
     * Creates a workflow step definition with all fields.
     */
    public WorkflowStepDefinition(String stepId, String name, Integer order, String type) {
        this.stepId = stepId;
        this.name = name;
        this.order = order;
        this.type = type;
    }

    // Getters and Setters

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "WorkflowStepDefinition{" +
                "stepId='" + stepId + '\'' +
                ", name='" + name + '\'' +
                ", order=" + order +
                ", type='" + type + '\'' +
                '}';
    }
}
