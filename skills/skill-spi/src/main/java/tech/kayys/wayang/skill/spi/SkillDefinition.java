package tech.kayys.gollek.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Skill Definition — the data-driven replacement for hardcoded agent executor
 * classes.
 *
 * <p>
 * A skill captures everything that makes an agent unique: its persona (system
 * prompt),
 * sub-skill prompts for different task types, inference parameters, tool
 * access, and metadata.
 *
 * <p>
 * Skills can be:
 * <ul>
 * <li><b>built-in</b> — shipped as classpath resources (coder, planner,
 * analytics, etc.)</li>
 * <li><b>template</b> — pre-defined templates users can customize</li>
 * <li><b>custom</b> — user-created via the UI canvas</li>
 * </ul>
 *
 * <p>
 * On the UI canvas, users drag an "Agent" node and assign a skill (or create a
 * new one).
 * The unified {@code SkillBasedAgentExecutor} reads the skill definition at
 * runtime
 * and uses it to drive inference — no new Java code needed.
 *
 * @param id                 Unique skill identifier, e.g. "coder",
 *                           "security-keycloak-expert"
 * @param name               Human-readable display name
 * @param description        When to use this agent / skill
 * @param category           One of: "built-in", "template", "custom"
 * @param systemPrompt       The main persona / system instructions
 * @param subSkillPrompts    Task-type-specific prompts, e.g. {"GENERATE":
 *                           "...", "REVIEW": "..."}
 * @param userPromptTemplate Template for rendering user prompts; supports
 *                           {{instruction}}, {{context}} placeholders
 * @param temperature        Default inference temperature
 * @param maxTokens          Default max tokens
 * @param defaultProvider    Default LLM provider ID
 * @param fallbackProvider   Fallback LLM provider ID
 * @param tools              Tool IDs this agent is allowed to use
 * @param orchestration      Orchestration config (only for orchestrator-type
 *                           skills)
 * @param metadata           Additional properties: color, icon, tags, version,
 *                           author, etc.
 */
public record SkillDefinition(
        String id,
        String name,
        String description,
        String category,
        String systemPrompt,
        Map<String, String> subSkillPrompts,
        String userPromptTemplate,
        Double temperature,
        Integer maxTokens,
        String defaultProvider,
        String fallbackProvider,
        List<String> tools,
        OrchestrationConfig orchestration,
        Map<String, Object> metadata) {

    /**
     * Defensive copy constructor.
     */
    public SkillDefinition {
        subSkillPrompts = subSkillPrompts != null ? Map.copyOf(subSkillPrompts) : Map.of();
        tools = tools != null ? List.copyOf(tools) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Whether this skill has sub-skill prompts for different task types.
     */
    public boolean hasSubSkills() {
        return !subSkillPrompts.isEmpty();
    }

    /**
     * Whether this skill is an orchestrator (has orchestration config).
     */
    public boolean isOrchestrator() {
        return orchestration != null;
    }

    /**
     * Get the effective system prompt for a given task type.
     * Falls back to the main system prompt if no sub-skill prompt matches.
     */
    public String effectiveSystemPrompt(String taskType) {
        if (taskType != null && subSkillPrompts.containsKey(taskType.toUpperCase())) {
            return subSkillPrompts.get(taskType.toUpperCase());
        }
        return systemPrompt;
    }

    /**
     * Orchestration configuration for orchestrator-type skills.
     *
     * @param defaultType        Default orchestration type: SEQUENTIAL, PARALLEL,
     *                           COLLABORATIVE, etc.
     * @param defaultStrategy    Default coordination strategy: CENTRALIZED,
     *                           DISTRIBUTED, HIERARCHICAL
     * @param defaultChildSkills Skill IDs auto-spawned when this orchestrator is
     *                           created
     * @param maxIterations      Max orchestration iterations
     * @param maxDelegations     Max delegation count
     */
    public record OrchestrationConfig(
            String defaultType,
            String defaultStrategy,
            List<String> defaultChildSkills,
            Integer maxIterations,
            Integer maxDelegations) {

        public OrchestrationConfig {
            defaultChildSkills = defaultChildSkills != null ? List.copyOf(defaultChildSkills) : List.of();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String category = "custom";
        private String systemPrompt;
        private Map<String, String> subSkillPrompts = Map.of();
        private String userPromptTemplate;
        private Double temperature = 0.7;
        private Integer maxTokens = 2048;
        private String defaultProvider;
        private String fallbackProvider;
        private List<String> tools = List.of();
        private OrchestrationConfig orchestration;
        private Map<String, Object> metadata = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder subSkillPrompts(Map<String, String> subSkillPrompts) {
            this.subSkillPrompts = subSkillPrompts;
            return this;
        }

        public Builder userPromptTemplate(String userPromptTemplate) {
            this.userPromptTemplate = userPromptTemplate;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder defaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
            return this;
        }

        public Builder fallbackProvider(String fallbackProvider) {
            this.fallbackProvider = fallbackProvider;
            return this;
        }

        public Builder tools(List<String> tools) {
            this.tools = tools;
            return this;
        }

        public Builder orchestration(OrchestrationConfig orchestration) {
            this.orchestration = orchestration;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SkillDefinition build() {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("Skill id is required");
            if (systemPrompt == null || systemPrompt.isBlank())
                throw new IllegalArgumentException("System prompt is required");
            return new SkillDefinition(id, name, description, category, systemPrompt,
                    subSkillPrompts, userPromptTemplate, temperature, maxTokens,
                    defaultProvider, fallbackProvider, tools, orchestration, metadata);
        }
    }
}
