package tech.kayys.wayang.agent.spi.skills;

import java.util.HashMap;
import java.util.Map;

public record SkillResult(
        String skillId,
        String invocationId,
        Status status,
        String observation,
        boolean success,
        Map<String, Object> memoryUpdates,
        Map<String, Object> outputData,
        long durationMs,
        Throwable error) {
    public enum Status {
        SUCCESS, FAILURE, ERROR
    }

    public static SkillResult success(String id, String obs) {
        return builder().skillId(id).status(Status.SUCCESS).observation(obs).build();
    }

    public static SkillResult failure(String id, String obs) {
        return builder().skillId(id).status(Status.FAILURE).observation(obs).build();
    }

    public static SkillResult failure(String id, Throwable err) {
        return builder().skillId(id).status(Status.ERROR).observation(err.getMessage()).error(err).build();
    }

    public boolean hasMemoryUpdates() {
        return memoryUpdates != null && !memoryUpdates.isEmpty();
    }

    public String getObservation() {
        return observation;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public Map<String, Object> getMemoryUpdates() {
        return memoryUpdates != null ? memoryUpdates : Map.of();
    }

    public Map<String, Object> getOutputs() {
        return outputData != null ? outputData : Map.of();
    }

    public boolean hasOutputs() {
        return outputData != null && !outputData.isEmpty();
    }

    public <T> T getOutput(String name, Class<T> type) {
        if (outputData == null) return null;
        Object val = outputData.get(name);
        return val != null ? type.cast(val) : null;
    }

    public Throwable getError() {
        return error;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String skillId;
        private String invocationId;
        private Status status = Status.SUCCESS;
        private String observation;
        private final Map<String, Object> memoryUpdates = new HashMap<>();
        private final Map<String, Object> outputData = new HashMap<>();
        private long durationMs;
        private Throwable error;

        public Builder skillId(String v) {
            skillId = v;
            return this;
        }

        public Builder invocationId(String v) {
            invocationId = v;
            return this;
        }

        public Builder status(Status v) {
            status = v;
            return this;
        }

        public Builder observation(String v) {
            observation = v;
            return this;
        }

        public Builder memoryUpdate(String k, Object v) {
            memoryUpdates.put(k, v);
            return this;
        }

        public Builder output(String k, Object v) {
            outputData.put(k, v);
            return this;
        }

        public Builder durationMs(long v) {
            durationMs = v;
            return this;
        }

        public Builder error(Throwable v) {
            error = v;
            status = Status.ERROR;
            return this;
        }

        public SkillResult build() {
            return new SkillResult(skillId, invocationId, status, observation, status == Status.SUCCESS,
                    memoryUpdates, outputData, durationMs, error);
        }
    }
}
