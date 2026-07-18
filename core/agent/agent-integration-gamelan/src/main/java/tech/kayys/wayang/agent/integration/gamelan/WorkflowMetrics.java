package tech.kayys.wayang.agent.integration.gamelan;

/**
 * Metrics about workflow execution for an agent.
 *
 * Tracks success rate, completion time, and failure counts to enable
 * learning and optimization of workflow selection and parameters.
 */
public class WorkflowMetrics {

    private int totalRuns;
    private int completedRuns;
    private int failedRuns;
    private double averageDurationMs;
    private double successRate;

    /**
     * Creates empty metrics.
     */
    public WorkflowMetrics() {
        this.totalRuns = 0;
        this.completedRuns = 0;
        this.failedRuns = 0;
        this.averageDurationMs = 0.0;
        this.successRate = 0.0;
    }

    /**
     * Creates metrics with all values.
     */
    public WorkflowMetrics(
            int totalRuns,
            int completedRuns,
            int failedRuns,
            double averageDurationMs,
            double successRate) {
        this.totalRuns = totalRuns;
        this.completedRuns = completedRuns;
        this.failedRuns = failedRuns;
        this.averageDurationMs = averageDurationMs;
        this.successRate = successRate;
    }

    // Getters and Setters

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }

    public int getCompletedRuns() {
        return completedRuns;
    }

    public void setCompletedRuns(int completedRuns) {
        this.completedRuns = completedRuns;
    }

    public int getFailedRuns() {
        return failedRuns;
    }

    public void setFailedRuns(int failedRuns) {
        this.failedRuns = failedRuns;
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
        return "WorkflowMetrics{" +
                "totalRuns=" + totalRuns +
                ", completedRuns=" + completedRuns +
                ", failedRuns=" + failedRuns +
                ", averageDurationMs=" + averageDurationMs +
                ", successRate=" + successRate +
                '}';
    }
}
