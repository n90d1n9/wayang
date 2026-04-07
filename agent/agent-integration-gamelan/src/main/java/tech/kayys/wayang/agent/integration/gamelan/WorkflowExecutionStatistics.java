package tech.kayys.wayang.agent.integration.gamelan.graph;

/**
 * Execution statistics for a workflow.
 * Tracks success rates, execution counts, and performance metrics.
 */
public class WorkflowExecutionStatistics {

    private String workflowId;
    private int totalExecutions;
    private int completedExecutions;
    private int failedExecutions;
    private double averageDurationMs;
    private double successRate;

    /**
     * Creates empty statistics.
     */
    public WorkflowExecutionStatistics() {
        this.totalExecutions = 0;
        this.completedExecutions = 0;
        this.failedExecutions = 0;
        this.averageDurationMs = 0.0;
        this.successRate = 0.0;
    }

    /**
     * Creates statistics with all values.
     */
    public WorkflowExecutionStatistics(
            String workflowId,
            int totalExecutions,
            int completedExecutions,
            int failedExecutions,
            double averageDurationMs,
            double successRate) {
        this.workflowId = workflowId;
        this.totalExecutions = totalExecutions;
        this.completedExecutions = completedExecutions;
        this.failedExecutions = failedExecutions;
        this.averageDurationMs = averageDurationMs;
        this.successRate = successRate;
    }

    // Getters and Setters

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public int getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(int totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public int getCompletedExecutions() {
        return completedExecutions;
    }

    public void setCompletedExecutions(int completedExecutions) {
        this.completedExecutions = completedExecutions;
    }

    public int getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(int failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public double getAverageDurationMs() {
        return averageDurationMs;
    }

    public void setAverageDurationMs(double averageDurationMs) {
        this.averageDurationMs = averageDurationMs;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    @Override
    public String toString() {
        return "WorkflowExecutionStatistics{" +
                "workflowId='" + workflowId + '\'' +
                ", totalExecutions=" + totalExecutions +
                ", completedExecutions=" + completedExecutions +
                ", failedExecutions=" + failedExecutions +
                ", averageDurationMs=" + averageDurationMs +
                ", successRate=" + successRate +
                '}';
    }
}
