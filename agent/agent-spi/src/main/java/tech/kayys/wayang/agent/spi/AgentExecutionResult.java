package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Agent Execution Result - Outcome of a task execution.
 */
public record AgentExecutionResult(
        String taskId,
        boolean success,
        String response,
        List<String> actionstaken,
        Map<String, Object> output,
        List<String> errors) {
}
