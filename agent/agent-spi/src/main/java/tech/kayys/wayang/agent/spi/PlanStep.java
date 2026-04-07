package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Plan Step - Single step in an execution plan.
 */
public record PlanStep(
        String stepId,
        String description,
        String tool,
        Map<String, Object> params,
        List<String> dependencies,
        String status,
        Map<String, Object> result) {

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }
}
