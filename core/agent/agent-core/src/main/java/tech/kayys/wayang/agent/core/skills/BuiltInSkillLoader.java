package tech.kayys.wayang.agent.core.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Loads built-in skill definitions from classpath JSON files on startup.
 *
 * <p>
 * Built-in skills are shipped as {@code skills/*.json} resources inside the
 * {@code wayang-agent-core} JAR. They replicate the capabilities that were
 * previously hardcoded in individual agent executor modules (coder, planner,
 * analytics, evaluator, orchestrator, common).
 *
 * <p>
 * Users can override or extend these skills at runtime via the
 * {@link SkillRegistry} API or through the UI canvas.
 */
@ApplicationScoped
@Startup
public class BuiltInSkillLoader {

    private static final Logger log = LoggerFactory.getLogger(BuiltInSkillLoader.class);

    private static final List<String> BUILT_IN_SKILLS = List.of(
            "skills/common.json",
            "skills/coder.json",
            "skills/planner.json",
            "skills/analytics.json",
            "skills/evaluator.json",
            "skills/orchestrator.json",
            // internal assistant skill – provides platform knowledge and project generation
            "skills/wayang-assistant.json");

    @Inject
    SkillRegistry skillRegistry;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Load all built-in skill definitions from classpath resources.
     * Called automatically on CDI startup.
     */
    @jakarta.annotation.PostConstruct
    void loadBuiltInSkills() {
        log.info("Loading built-in agent skills...");
        int loaded = 0;

        for (String resourcePath : BUILT_IN_SKILLS) {
            try {
                SkillDefinition skill = loadSkillFromResource(resourcePath);
                if (skill != null) {
                    skillRegistry.registerSkill(skill);
                    loaded++;
                }
            } catch (Exception e) {
                log.warn("Failed to load built-in skill from {}: {}", resourcePath, e.getMessage());
            }
        }

        log.info("Loaded {} built-in agent skills", loaded);
    }

    /**
     * Load a single skill definition from a classpath JSON resource.
     */
    @SuppressWarnings("unchecked")
    private SkillDefinition loadSkillFromResource(String resourcePath) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Built-in skill resource not found: {}", resourcePath);
                return null;
            }

            Map<String, Object> json = objectMapper.readValue(is, Map.class);

            var builder = SkillDefinition.builder()
                    .id((String) json.get("id"))
                    .name((String) json.get("name"))
                    .description((String) json.get("description"))
                    .category((String) json.getOrDefault(SkillMetadataKeys.KEY_CATEGORY, "built-in"))
                    .systemPrompt((String) json.get("systemPrompt"));

            // Sub-skill prompts
            if (json.containsKey("subSkillPrompts")) {
                builder.subSkillPrompts((Map<String, String>) json.get("subSkillPrompts"));
            }

            // User prompt template
            if (json.containsKey("userPromptTemplate")) {
                builder.userPromptTemplate((String) json.get("userPromptTemplate"));
            }

            // Inference parameters
            if (json.containsKey("temperature")) {
                builder.temperature(((Number) json.get("temperature")).doubleValue());
            }
            if (json.containsKey("maxTokens")) {
                builder.maxTokens(((Number) json.get("maxTokens")).intValue());
            }
            if (json.containsKey("defaultProvider")) {
                builder.defaultProvider((String) json.get("defaultProvider"));
            }
            if (json.containsKey("fallbackProvider")) {
                builder.fallbackProvider((String) json.get("fallbackProvider"));
            }

            // Tools
            if (json.containsKey("tools")) {
                builder.tools((List<String>) json.get("tools"));
            }

            // Orchestration config
            if (json.containsKey("orchestration")) {
                Map<String, Object> orchConfig = (Map<String, Object>) json.get("orchestration");
                builder.orchestration(new SkillDefinition.OrchestrationConfig(
                        (String) orchConfig.get("defaultType"),
                        (String) orchConfig.get("defaultStrategy"),
                        orchConfig.containsKey("defaultChildSkills")
                                ? (List<String>) orchConfig.get("defaultChildSkills")
                                : List.of(),
                        orchConfig.containsKey("maxIterations")
                                ? ((Number) orchConfig.get("maxIterations")).intValue()
                                : null,
                        orchConfig.containsKey("maxDelegations")
                                ? ((Number) orchConfig.get("maxDelegations")).intValue()
                                : null));
            }

            // Metadata
            if (json.containsKey("metadata")) {
                builder.metadata((Map<String, Object>) json.get("metadata"));
            }

            return builder.build();
        }
    }
}
