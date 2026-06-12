package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import io.smallrye.mutiny.Uni;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Prompt-aware skill context provider.
 * 
 * Enriches skill context with prompt engineering metadata, allowing skills
 * to participate in prompt construction and injection workflows.
 */
public class PromptContextProvider {

    private final SkillContext context;
    private final Map<String, String> promptAttributes;

    public PromptContextProvider(SkillContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.promptAttributes = new LinkedHashMap<>();
    }

    /**
     * Inject prompt context into skill execution.
     */
    public PromptContextProvider withPromptContext(String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            promptAttributes.put(key.trim(), value);
        }
        return this;
    }

    /**
     * Get skill metadata for prompt engineering.
     */
    public Optional<String> getPromptTemplate() {
        String template = promptAttributes.get(SkillContextKeys.KEY_PROMPT_TEMPLATE);
        return template == null || template.isBlank() ? Optional.empty() : Optional.of(template);
    }

    /**
     * Enhance prompt with skill metadata.
     */
    public String enrichPrompt(String basePrompt) {
        StringBuilder enhanced = new StringBuilder(basePrompt == null ? "" : basePrompt);
        SkillMetadata metadata = context.metadata();
        
        if (metadata != null) {
            enhanced.append("\n\n## Available Skill: ").append(metadata.name());
            if (metadata.description() != null && !metadata.description().isBlank()) {
                enhanced.append("\n").append(metadata.description());
            }
            
            if (!metadata.tags().isEmpty()) {
                enhanced.append("\nTags: ").append(String.join(", ", metadata.tags()));
            }
        }
        
        return enhanced.toString();
    }

    /**
     * Extract prompt variables from skill context.
     */
    public Map<String, String> getPromptVariables() {
        Map<String, String> vars = new LinkedHashMap<>(promptAttributes);
        putIfPresent(vars, SkillContextKeys.KEY_SKILL_ID, context.skillId());
        SkillMetadata metadata = context.metadata();
        if (metadata != null) {
            putIfPresent(vars, SkillContextKeys.KEY_SKILL_NAME, metadata.name());
            putIfPresent(vars, SkillContextKeys.KEY_SKILL_DESCRIPTION, metadata.description());
            putIfPresent(vars, SkillContextKeys.KEY_SKILL_VERSION, metadata.version());
            if (!metadata.tags().isEmpty()) {
                vars.put(SkillContextKeys.KEY_SKILL_TAGS, String.join(",", metadata.tags()));
            }
        }
        return Map.copyOf(vars);
    }

    /**
     * Create prompt-aware skill context asynchronously.
     */
    public Uni<PromptContextProvider> enrich() {
        return Uni.createFrom().item(this);
    }

    private static void putIfPresent(Map<String, String> variables, String key, String value) {
        if (value != null && !value.isBlank()) {
            variables.put(key, value);
        }
    }
}
