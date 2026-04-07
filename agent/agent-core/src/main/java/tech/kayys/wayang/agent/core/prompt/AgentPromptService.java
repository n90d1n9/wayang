package tech.kayys.wayang.agent.core.prompt;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.core.memory.AgentMemoryService;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt Management Service with Memory Integration
 *
 * Bridges prompt management with agent execution to provide:
 * - Dynamic prompt rendering with variables
 * - Multi-model prompt templates
 * - Version control and history
 * - Context injection from memory
 * - Prompt optimization tracking
 *
 * Usage:
 * {@code
 * @Inject
 * AgentPromptService promptService;
 *
 * // Get prompt template
 * PromptTemplate template = promptService
 *     .getTemplate("react_agent")
 *     .await().indefinitely();
 *
 * // Render with context
 * String prompt = promptService.renderPrompt(template,
 *     Map.of("task", "Find latest news", "context", "Previous queries..."))
 *     .await().indefinitely();
 * }
 */
@ApplicationScoped
public class AgentPromptService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentPromptService.class);

    @Inject
    AgentMemoryService memoryService;

    /**
     * Get prompt template by ID
     *
     * @param templateId The template ID
     * @return Reactive prompt template
     */
    public Uni<PromptTemplate> getTemplate(String templateId) {
        LOG.debug("Loading prompt template: {}", templateId);

        return Uni.createFrom().item(() -> {
            // In production: Load from database/file system
            return PromptTemplate.builder()
                    .id(templateId)
                    .name("Template: " + templateId)
                    .description("Prompt template for " + templateId)
                    .template("Default template content\nTask: {{task}}\nContext: {{context}}")
                    .version("1.0")
                    .modelCompatibility(List.of("openai-gpt4", "anthropic-claude3"))
                    .variables(List.of("task", "context", "tools", "history"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });
    }

    /**
     * Render prompt template with context variables
     *
     * @param template The prompt template
     * @param variables Variable values to substitute
     * @return Rendered prompt string
     */
    public Uni<String> renderPrompt(PromptTemplate template, Map<String, Object> variables) {
        return Uni.createFrom().item(() -> {
            String rendered = template.template();

            // Replace variables in template
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String varPattern = "\\{\\{" + entry.getKey() + "\\}\\}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                rendered = rendered.replaceAll(varPattern, Matcher.quoteReplacement(value));
            }

            LOG.debug("Rendered prompt for template {} with {} variables",
                    template.id(), variables.size());

            return rendered;
        });
    }

    /**
     * Enhance prompt with agent memory context
     *
     * @param agentId The agent ID
     * @param template The prompt template
     * @param baseVariables Base variables
     * @return Enhanced prompt with memory context
     */
    public Uni<String> enhanceWithMemory(
            String agentId,
            PromptTemplate template,
            Map<String, Object> baseVariables) {

        return memoryService.getContextPrompt(agentId, 5)
                .map(memoryContext -> {
                    Map<String, Object> enhanced = new HashMap<>(baseVariables);
                    enhanced.put("history", memoryContext);
                    enhanced.put("agentId", agentId);
                    return enhanced;
                })
                .flatMap(enriched -> renderPrompt(template, enriched));
    }

    /**
     * Get optimized prompt for model
     *
     * @param templateId Template ID
     * @param modelName Target model (e.g., "openai-gpt4")
     * @param variables Variables
     * @return Optimized prompt for model
     */
    public Uni<String> getOptimizedPrompt(
            String templateId,
            String modelName,
            Map<String, Object> variables) {

        return getTemplate(templateId)
                .flatMap(template -> {
                    if (!template.modelCompatibility().contains(modelName)) {
                        LOG.warn("Template {} not optimized for {}", templateId, modelName);
                    }

                    // Apply model-specific optimizations
                    PromptTemplate optimized = optimizeForModel(template, modelName);
                    return renderPrompt(optimized, variables);
                });
    }

    /**
     * Create system prompt for agent
     *
     * @param agentId Agent ID
     * @param agentDescription What the agent does
     * @param tools Available tools
     * @param context Memory context
     * @return Complete system prompt
     */
    public Uni<String> createSystemPrompt(
            String agentId,
            String agentDescription,
            List<String> tools,
            String context) {

        String systemPrompt = """
                You are an AI agent: %s
                
                Your responsibilities:
                - %s
                
                Available tools:
                %s
                
                Previous context:
                %s
                
                Guidelines:
                1. Think step-by-step before acting
                2. Use tools when necessary
                3. Store important information for future use
                4. Ask for clarification when needed
                """.formatted(
                agentId,
                agentDescription,
                String.join("\n", tools),
                context
        );

        // Store in memory for audit trail
        return storePromptUsage(agentId, "system_prompt", systemPrompt)
                .map(__ -> systemPrompt);
    }

    /**
     * Get prompt analytics for agent
     *
     * @param agentId The agent ID
     * @return Prompt usage statistics
     */
    public Uni<PromptAnalytics> getAnalytics(String agentId) {
        return memoryService.getSessionMemories(agentId, null, 100)
                .map(memories -> {
                    int promptCount = (int) memories.stream()
                            .filter(m -> m.getMetadata().get("type").equals("prompt-usage"))
                            .count();

                    return new PromptAnalytics(
                            agentId,
                            promptCount,
                            promptCount > 0 ? promptCount / 10 : 0,  // avg length
                            0.0,  // avg cost
                            Instant.now()
                    );
                });
    }

    /**
     * Version control: Get prompt history
     *
     * @param templateId Template ID
     * @return Reactive list of versions
     */
    public Uni<List<PromptTemplate>> getVersionHistory(String templateId) {
        return Uni.createFrom().item(() -> {
            // In production: Query version control system
            LOG.debug("Getting version history for template: {}", templateId);
            return new ArrayList<>();
        });
    }

    /**
     * Create new prompt template
     *
     * @param builder Builder with template details
     * @return Created template
     */
    public Uni<PromptTemplate> createTemplate(PromptTemplate.Builder builder) {
        return Uni.createFrom().item(builder.build())
                .invoke(template -> {
                    LOG.info("Created prompt template: {} v{}", template.id(), template.version());
                });
    }

    /**
     * Validate prompt template syntax
     *
     * @param template Template to validate
     * @return Validation result
     */
    public Uni<PromptValidation> validateTemplate(PromptTemplate template) {
        return Uni.createFrom().item(() -> {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Check for unmatched placeholders
            Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
            Matcher matcher = pattern.matcher(template.template());
            Set<String> foundVars = new HashSet<>();
            while (matcher.find()) {
                foundVars.add(matcher.group(1));
            }

            // Check if all required variables are defined
            for (String found : foundVars) {
                if (!template.variables().contains(found)) {
                    warnings.add("Variable '" + found + "' used but not declared");
                }
            }

            return new PromptValidation(
                    template.id(),
                    errors.isEmpty() && warnings.isEmpty(),
                    errors,
                    warnings
            );
        });
    }

    // Helper methods

    private PromptTemplate optimizeForModel(PromptTemplate template, String modelName) {
        // Model-specific optimizations
        String optimized = template.template();

        if (modelName.contains("gpt")) {
            // OpenAI specific optimizations
            optimized = optimized.replace("{{tools}}", "Tool definitions in JSON format");
        } else if (modelName.contains("claude")) {
            // Anthropic specific optimizations
            optimized = optimized.replace("{{tools}}", "Tool definitions in text format");
        }

        return PromptTemplate.builder()
                .id(template.id())
                .name(template.name())
                .template(optimized)
                .version(template.version())
                .modelCompatibility(template.modelCompatibility())
                .variables(template.variables())
                .createdAt(template.createdAt())
                .updatedAt(Instant.now())
                .build();
    }

    private Uni<Void> storePromptUsage(String agentId, String promptType, String content) {
        MemoryEntry entry = new MemoryEntry(
                UUID.randomUUID().toString(),
                "Prompt: " + promptType + " | Length: " + content.length(),
                Instant.now(),
                Map.of(
                        "agentId", agentId,
                        "type", "prompt-usage",
                        "promptType", promptType,
                        "length", String.valueOf(content.length()),
                        "source", "agent-prompt-service"
                )
        );

        return memoryService.vectorAgentMemory()
                .store(agentId, entry)
                .onFailure().recoverWithVoid();
    }

    /**
     * Prompt Template record
     */
    public static class PromptTemplate {
        private final String id;
        private final String name;
        private final String description;
        private final String template;
        private final String version;
        private final List<String> modelCompatibility;
        private final List<String> variables;
        private final Instant createdAt;
        private final Instant updatedAt;

        public PromptTemplate(String id, String name, String description, String template,
                            String version, List<String> modelCompatibility,
                            List<String> variables, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.template = template;
            this.version = version;
            this.modelCompatibility = modelCompatibility;
            this.variables = variables;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String id() { return id; }
        public String name() { return name; }
        public String description() { return description; }
        public String template() { return template; }
        public String version() { return version; }
        public List<String> modelCompatibility() { return modelCompatibility; }
        public List<String> variables() { return variables; }
        public Instant createdAt() { return createdAt; }
        public Instant updatedAt() { return updatedAt; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private String name;
            private String description;
            private String template;
            private String version = "1.0";
            private List<String> modelCompatibility = new ArrayList<>();
            private List<String> variables = new ArrayList<>();
            private Instant createdAt = Instant.now();
            private Instant updatedAt = Instant.now();

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

            public Builder template(String template) {
                this.template = template;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder modelCompatibility(List<String> models) {
                this.modelCompatibility = models;
                return this;
            }

            public Builder variables(List<String> variables) {
                this.variables = variables;
                return this;
            }

            public PromptTemplate build() {
                return new PromptTemplate(id, name, description, template, version,
                                        modelCompatibility, variables, createdAt, updatedAt);
            }
        }
    }

    /**
     * Prompt Analytics record
     */
    public record PromptAnalytics(
            String agentId,
            int totalPrompts,
            int avgPromptLength,
            double estimatedCost,
            Instant analyzedAt) {
    }

    /**
     * Prompt Validation record
     */
    public record PromptValidation(
            String templateId,
            boolean isValid,
            List<String> errors,
            List<String> warnings) {

        public String getSummary() {
            if (isValid) {
                return "Valid template";
            } else {
                return "Invalid: " + String.join(", ", errors);
            }
        }
    }
}
