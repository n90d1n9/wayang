package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
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
        this.context = context;
        this.promptAttributes = new HashMap<>();
    }

    /**
     * Inject prompt context into skill execution.
     */
    public PromptContextProvider withPromptContext(String key, String value) {
        promptAttributes.put(key, value);
        return this;
    }

    /**
     * Get skill metadata for prompt engineering.
     */
    public Optional<String> getPromptTemplate() {
        return Optional.ofNullable(promptAttributes.get("template"));
    }

    /**
     * Enhance prompt with skill metadata.
     */
    public String enrichPrompt(String basePrompt) {
        StringBuilder enhanced = new StringBuilder(basePrompt);
        
        if (context.metadata() != null) {
            enhanced.append("\n\n## Available Skill: ").append(context.metadata().name());
            enhanced.append("\n").append(context.metadata().description());
            
            if (!context.metadata().tags().isEmpty()) {
                enhanced.append("\nTags: ").append(String.join(", ", context.metadata().tags()));
            }
        }
        
        return enhanced.toString();
    }

    /**
     * Extract prompt variables from skill context.
     */
    public Map<String, String> getPromptVariables() {
        Map<String, String> vars = new HashMap<>(promptAttributes);
        if (context.metadata() != null) {
            vars.put("skillName", context.metadata().name());
            vars.put("skillDescription", context.metadata().description());
            vars.put("skillVersion", context.metadata().version());
        }
        return vars;
    }

    /**
     * Create prompt-aware skill context asynchronously.
     */
    public Uni<PromptContextProvider> enrich() {
        return Uni.createFrom().item(this);
    }
}
