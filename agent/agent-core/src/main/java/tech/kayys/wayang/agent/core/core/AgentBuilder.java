package tech.kayys.wayang.agent.core.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent Agent Builder — the primary entry point for composing agents.
 *
 * <p>
 * Provides a high-level DSL for defining agents without dealing with
 * low-level request/state objects. Supports method chaining, inline skill
 * registration, and strategy configuration.
 *
 * <h3>Usage — Simple Query</h3>
 * 
 * <pre>{@code
 * AgentResponse response = agentBuilder
 *         .newAgent()
 *         .withPrompt("Summarize the latest AI research trends")
 *         .usingSkills("web-search", "summarization")
 *         .withMaxSteps(8)
 *         .forTenant("enterprise")
 *         .execute()
 *         .await().indefinitely();
 * }</pre>
 *
 * <h3>Usage — Data Analysis Pipeline</h3>
 * 
 * <pre>{@code
 * Uni<AgentResponse> result = agentBuilder
 *         .newAgent()
 *         .withPrompt("Analyze sales data and identify top performing regions")
 *         .withContext("data", csvContent)
 *         .usingStrategy(OrchestrationStrategy.PLAN_AND_EXECUTE)
 *         .usingSkills("data-analysis", "summarization", "sql-query")
 *         .withModel("Qwen/Qwen2.5-7B-Instruct")
 *         .withTimeout(Duration.ofMinutes(5))
 *         .forTenant("analytics-team")
 *         .executeAsync();
 * }</pre>
 *
 * @author Bhangun
 */
@ApplicationScoped
public class AgentBuilder {

    private static final Logger LOG = Logger.getLogger(AgentBuilder.class);

    @Inject
    Map<String, AgentOrchestrator> orchestrators;

    @Inject
    SkillRegistry skillRegistry;

    /** Start building a new agent. */
    public FluentAgent newAgent() {
        return new FluentAgent(orchestrators, skillRegistry);
    }

    /** Start building a ReAct agent (shorthand). */
    public FluentAgent react(String prompt) {
        return newAgent()
                .withPrompt(prompt)
                .usingStrategy(OrchestrationStrategy.REACT);
    }

    /** Start building a Plan-and-Execute agent (shorthand). */
    public FluentAgent planAndExecute(String prompt) {
        return newAgent()
                .withPrompt(prompt)
                .usingStrategy(OrchestrationStrategy.PLAN_AND_EXECUTE);
    }

    // ── Fluent Agent ──────────────────────────────────────────────────────────

    public static final class FluentAgent {

        private final AgentRequest.Builder requestBuilder = AgentRequest.builder();
        private final Map<String, AgentOrchestrator> orchestrators;
        private final SkillRegistry skillRegistry;

        // Track inline skill registrations
        private final List<AgentSkill> inlineSkills = new ArrayList<>();

        FluentAgent(Map<String, AgentOrchestrator> orchestrators, SkillRegistry skillRegistry) {
            this.orchestrators = orchestrators;
            this.skillRegistry = skillRegistry;
        }

        /** Set the user prompt (required). */
        public FluentAgent withPrompt(String prompt) {
            requestBuilder.prompt(prompt);
            return this;
        }

        /** Set a custom system prompt. */
        public FluentAgent withSystemPrompt(String systemPrompt) {
            requestBuilder.systemPrompt(systemPrompt);
            return this;
        }

        /** Select orchestration strategy. */
        public FluentAgent usingStrategy(OrchestrationStrategy strategy) {
            requestBuilder.strategy(strategy);
            return this;
        }

        /** Add a registered skill by ID. */
        public FluentAgent usingSkill(String skillId) {
            requestBuilder.skill(skillId);
            return this;
        }

        /** Add multiple registered skills by ID. */
        public FluentAgent usingSkills(String... skillIds) {
            Arrays.stream(skillIds).forEach(requestBuilder::skill);
            return this;
        }

        /** Add multiple registered skills by ID. */
        public FluentAgent usingSkills(List<String> skillIds) {
            skillIds.forEach(requestBuilder::skill);
            return this;
        }

        /** Allow ALL registered skills (no filter). */
        public FluentAgent usingAllSkills() {
            // Empty list = all allowed
            return this;
        }

        /** Add context data available to skills during execution. */
        public FluentAgent withContext(String key, Object value) {
            requestBuilder.context(key, value);
            return this;
        }

        /** Add multiple context values. */
        public FluentAgent withContext(Map<String, Object> ctx) {
            requestBuilder.context(ctx);
            return this;
        }

        /** Override the LLM model. */
        public FluentAgent withModel(String modelId) {
            requestBuilder.modelId(modelId);
            return this;
        }

        /** Set model parameters. */
        public FluentAgent withModelParameter(String key, Object value) {
            requestBuilder.modelParameter(key, value);
            return this;
        }

        /** Set maximum reasoning steps. */
        public FluentAgent withMaxSteps(int maxSteps) {
            requestBuilder.maxSteps(maxSteps);
            return this;
        }

        /** Set execution timeout. */
        public FluentAgent withTimeout(Duration timeout) {
            requestBuilder.timeout(timeout);
            return this;
        }

        /** Set tenant context. */
        public FluentAgent forTenant(String tenantId) {
            requestBuilder.tenantId(tenantId);
            return this;
        }

        /** Set user ID. */
        public FluentAgent forUser(String userId) {
            requestBuilder.userId(userId);
            return this;
        }

        /** Set session ID for conversation continuity. */
        public FluentAgent withSession(String sessionId) {
            requestBuilder.sessionId(sessionId);
            return this;
        }

        /** Enable streaming output. */
        public FluentAgent streaming() {
            requestBuilder.stream(true);
            return this;
        }

        /** Enable verbose logging of reasoning steps. */
        public FluentAgent verbose() {
            requestBuilder.verbose(true);
            return this;
        }

        /** Configure memory settings. */
        public FluentAgent withMemory(Consumer<AgentMemoryConfig> memConfig) {
            // Builder-style memory config; for simplicity use defaults
            requestBuilder.memoryConfig(AgentMemoryConfig.defaults());
            return this;
        }

        /** Register an inline skill for this agent only (not globally). */
        public FluentAgent withInlineSkill(AgentSkill skill) {
            inlineSkills.add(skill);
            requestBuilder.skill(skill.id());
            return this;
        }

        /** Build the AgentRequest. */
        public AgentRequest build() {
            // Register inline skills if any
            inlineSkills.forEach(skill -> {
                if (!skillRegistry.find(skill.id()).isPresent()) {
                    ((DefaultSkillRegistry) skillRegistry).register(skill);
                }
            });
            return requestBuilder.build();
        }

        /** Execute synchronously (blocks). */
        public AgentResponse executeBlocking() {
            return executeAsync().await().indefinitely();
        }

        /** Execute asynchronously. */
        public Uni<AgentResponse> executeAsync() {
            AgentRequest request = build();
            String strategyId = request.strategy().id;
            AgentOrchestrator orchestrator = orchestrators.get(strategyId);
            if (orchestrator == null) {
                // Fallback to react
                orchestrator = orchestrators.get("react");
            }
            if (orchestrator == null) {
                return Uni.createFrom().failure(
                        new IllegalStateException("No orchestrator found for strategy: " + strategyId));
            }
            LOG.infof("AgentBuilder: executing agent [strategy=%s, maxSteps=%d, tenant=%s]",
                    strategyId, request.getMaxSteps(), request.tenantId());
            return orchestrator.execute(request);
        }

        /** Execute (alias for executeAsync). */
        public Uni<AgentResponse> execute() {
            return executeAsync();
        }
    }
}
