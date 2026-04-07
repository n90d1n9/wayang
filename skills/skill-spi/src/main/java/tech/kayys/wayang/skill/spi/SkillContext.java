package tech.kayys.gollek.agent.spi;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record SkillContext(
        String skillId,
        String invocationId,
        String tenantId,
        Map<String, Object> inputs,
        Map<String, Object> agentContext,
        Map<String, Object> workingMemory,
        String runId,
        int stepNumber,
        Duration timeout) {
    public void requireInput(String name) {
        if (!inputs.containsKey(name) || inputs.get(name) == null) {
            throw new IllegalArgumentException("Missing required input: " + name);
        }
    }

    public <T> T requireInput(String name, Class<T> type) {
        Object val = inputs.get(name);
        if (val == null) {
            throw new IllegalArgumentException("Missing required input: " + name);
        }
        return type.cast(val);
    }

    public String getStringInput(String name) {
        return (String) inputs.get(name);
    }

    public String getStringInput(String name, String defaultValue) {
        return (String) inputs.getOrDefault(name, defaultValue);
    }

    public int getIntInput(String name, int defaultValue) {
        Object val = inputs.get(name);
        if (val instanceof Number n)
            return n.intValue();
        return defaultValue;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String skillId;
        private String invocationId;
        private String tenantId;
        private Map<String, Object> inputs;
        private Map<String, Object> agentContext;
        private Map<String, Object> workingMemory;
        private String runId;
        private int stepNumber;
        private Duration timeout;

        public Builder skillId(String v) {
            skillId = v;
            return this;
        }

        public Builder invocationId(String v) {
            invocationId = v;
            return this;
        }

        public Builder tenantId(String v) {
            tenantId = v;
            return this;
        }

        public Builder inputs(Map<String, Object> v) {
            inputs = v;
            return this;
        }

        public Builder agentContext(Map<String, Object> v) {
            agentContext = v;
            return this;
        }

        public Builder workingMemory(Map<String, Object> v) {
            workingMemory = v;
            return this;
        }

        public Builder runId(String v) {
            runId = v;
            return this;
        }

        public Builder stepNumber(int v) {
            stepNumber = v;
            return this;
        }

        public Builder timeout(Duration v) {
            timeout = v;
            return this;
        }

        public SkillContext build() {
            return new SkillContext(skillId,
                    invocationId != null ? invocationId : java.util.UUID.randomUUID().toString(),
                    tenantId, inputs, agentContext, workingMemory, runId, stepNumber, timeout);
        }
    }
}
