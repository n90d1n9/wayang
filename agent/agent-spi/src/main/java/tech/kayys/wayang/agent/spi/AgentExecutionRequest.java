package tech.kayys.wayang.agent.spi;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agent Execution Request
 */
public record AgentExecutionRequest(
        String requestId,
        String taskDescription,
        Map<String, Object> context,
        Set<String> requiredCapabilities,
        ExecutionConstraints constraints,
        Map<String, Object> metadata,
        String taskId,
        boolean success,
        String response,
        List<String> actionstaken,
        Map<String, Object> output,
        List<String> errors,
        Instant submittedAt) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String requestId = UUID.randomUUID().toString();
        private String taskDescription;
        private Map<String, Object> context = new HashMap<>();
        private Set<String> requiredCapabilities = new HashSet<>();
        private ExecutionConstraints constraints = ExecutionConstraints.createDefault();
        private Map<String, Object> metadata = new HashMap<>();
        private String taskId;
        private boolean success;
        private String response;
        private List<String> actionstaken;
        private Map<String, Object> output;
        private List<String> errors;

        public Builder taskDescription(String desc) {
            this.taskDescription = desc;
            return this;
        }

        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder requiredCapability(String capability) {
            this.requiredCapabilities.add(capability);
            return this;
        }

        public Builder constraints(ExecutionConstraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder actionstaken(List<String> actions) {
            this.actionstaken = actions;
            return this;
        }

        public Builder output(Map<String, Object> output) {
            this.output = output;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public AgentExecutionRequest build() {
            return new AgentExecutionRequest(
                    requestId,
                    taskDescription,
                    context,
                    requiredCapabilities,
                    constraints,
                    metadata,
                    taskId,
                    success,
                    response,
                    actionstaken,
                    output,
                    errors,
                    Instant.now());
        }
    }
}
