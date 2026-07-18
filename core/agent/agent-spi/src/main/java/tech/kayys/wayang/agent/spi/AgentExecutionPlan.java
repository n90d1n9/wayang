package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Agent Execution Plan
 */
public record AgentExecutionPlan(
        String planId,
        String description,
        List<PlanStep> steps,
        Map<String, Object> planContext,
        PlanMetadata metadata,
        Instant createdAt) {

    public boolean isComplete() {
        return steps.stream().allMatch(PlanStep::isCompleted);
    }

    public double getCompletionPercentage() {
        long completed = steps.stream().filter(PlanStep::isCompleted).count();
        return (double) completed / steps.size() * 100.0;
    }
}
