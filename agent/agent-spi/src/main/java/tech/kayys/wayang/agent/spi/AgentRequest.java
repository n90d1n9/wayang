package tech.kayys.wayang.agent.spi;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Immutable agent execution request.
 *
 * @param requestId      Unique request identifier
 * @param prompt         User input prompt
 * @param systemPrompt   Optional system prompt override
 * @param strategy       Orchestration strategy to use
 * @param allowedSkills  List of skill IDs the agent can call
 * @param context        Application-provided context data
 * @param parameters     Agent-specific parameters (maxSteps, etc)
 * @param tenantId       Tenant identifier
 * @param sessionId      Optional session identifier
 * @param userId         Optional user identifier
 * @param stream         Whether to stream events
 * @param verbose        Whether to enable verbose logging
 * @param timeout        Execution timeout
 * @param memoryConfig   Memory configuration
 * @param modelId        Optional LLM model override
 * @param timestamp      Record timestamp
 */
public record AgentRequest(
        String requestId,
        String prompt,
        String systemPrompt,
        OrchestrationStrategy strategy,
        List<String> allowedSkills,
        Map<String, Object> context,
        Map<String, Object> parameters,
        String tenantId,
        String sessionId,
        String userId,
        boolean stream,
        boolean verbose,
        Duration timeout,
        AgentMemoryConfig memoryConfig,
        String modelId,
        Instant timestamp) {

    public AgentRequest {
        requestId = requestId != null ? requestId : UUID.randomUUID().toString();
        allowedSkills = allowedSkills != null ? List.copyOf(allowedSkills) : List.of();
        context = context != null ? Map.copyOf(context) : Map.of();
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        timestamp = timestamp != null ? timestamp : Instant.now();
        strategy = strategy != null ? strategy : OrchestrationStrategy.REACT;
        timeout = timeout != null ? timeout : Duration.ofMinutes(2);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasSkillFilter() {
        return !allowedSkills.isEmpty();
    }

    public int getMaxSteps() {
        Object v = parameters.get("maxSteps");
        if (v instanceof Number n) return n.intValue();
        return 15;
    }

    public Duration getTimeout() {
        return timeout != null ? timeout : Duration.ofMinutes(2);
    }

    public static final class Builder {
        private String requestId;
        private String prompt;
        private String systemPrompt;
        private OrchestrationStrategy strategy = OrchestrationStrategy.REACT;
        private List<String> allowedSkills = new ArrayList<>();
        private Map<String, Object> context = new HashMap<>();
        private Map<String, Object> parameters = new HashMap<>();
        private String tenantId = "default";
        private String sessionId;
        private String userId;
        private boolean stream = false;
        private boolean verbose = false;
        private Duration timeout = Duration.ofMinutes(2);
        private AgentMemoryConfig memoryConfig = AgentMemoryConfig.defaults();
        private String modelId;

        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder prompt(String v) { this.prompt = v; return this; }
        public Builder systemPrompt(String v) { this.systemPrompt = v; return this; }
        public Builder strategy(OrchestrationStrategy v) { this.strategy = v; return this; }
        public Builder skill(String v) { this.allowedSkills.add(v); return this; }
        public Builder context(String k, Object v) { this.context.put(k, v); return this; }
        public Builder context(Map<String, Object> ctx) { this.context.putAll(ctx); return this; }
        public Builder parameter(String k, Object v) { this.parameters.put(k, v); return this; }
        public Builder parameters(Map<String, Object> params) { this.parameters.putAll(params); return this; }
        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder sessionId(String v) { this.sessionId = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder agentId(String v) { this.context.put("agentId", v); return this; }
        public Builder stream(boolean v) { this.stream = v; return this; }
        public Builder verbose(boolean v) { this.verbose = v; return this; }
        public Builder timeout(Duration v) { this.timeout = v; return this; }
        public Builder maxSteps(int v) { this.parameters.put("maxSteps", v); return this; }
        public Builder tools(Object v) { this.parameters.put("tools", v); return this; }
        public Builder memoryConfig(AgentMemoryConfig v) { this.memoryConfig = v; return this; }
        public Builder modelId(String v) { this.modelId = v; return this; }
        public Builder modelParameter(String k, Object v) {
            Map<String, Object> mp = (Map<String, Object>) this.parameters.getOrDefault("modelParameters", new HashMap<>());
            mp.put(k, v);
            this.parameters.put("modelParameters", mp);
            return this;
        }

        public AgentRequest build() {
            return new AgentRequest(requestId, prompt, systemPrompt, strategy,
                    allowedSkills, context, parameters, tenantId, sessionId, userId,
                    stream, verbose, timeout, memoryConfig, modelId, Instant.now());
        }
    }
}
