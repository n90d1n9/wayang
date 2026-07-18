package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.core.skills.loader.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutionOutcome;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.validation.SkillParameterSchema;

import java.util.*;

/**
 * Adapts a filesystem skill manifest to the agent's internal tool shape.
 *
 * <p>This is intentionally separate from {@link SkillAsToolAdapter}, which
 * adapts runtime {@code SkillDefinition} records to the public tool SPI.
 */
public class ManifestSkillToolAdapter {

    private final SkillManifest metadata;
    private final SkillExecutor executor;

    public ManifestSkillToolAdapter(SkillManifest metadata, SkillExecutor executor) {
        this.metadata = Objects.requireNonNull(metadata);
        this.executor = Objects.requireNonNull(executor);
    }

    /**
     * Get tool name (derived from skill name).
     */
    public String getToolName() {
        return metadata.getName();
    }

    /**
     * Get tool description.
     */
    public String getToolDescription() {
        return metadata.getDescription();
    }

    /**
     * Get tool parameters schema.
     */
    public Map<String, Object> getParameters() {
        return SkillParameterSchema.fromManifest(metadata).toJsonSchema();
    }

    /**
     * Execute the skill as a tool.
     */
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();

        try {
            SkillExecutionOutcome result = executor.executeSkill(metadata.getName(), parameters);

            if (result.success()) {
                return new ToolExecutionResult(
                        metadata.getName(),
                        result.output(),
                        true,
                        System.currentTimeMillis() - startTime,
                        null
                );
            } else {
                return new ToolExecutionResult(
                        metadata.getName(),
                        result.output(),
                        false,
                        System.currentTimeMillis() - startTime,
                        result.error()
                );
            }

        } catch (Exception e) {
            return new ToolExecutionResult(
                    metadata.getName(),
                    null,
                    false,
                    System.currentTimeMillis() - startTime,
                    e.getMessage()
            );
        }
    }

    /**
     * Create a ToolDefinition-compatible representation.
     */
    public ToolDefinitionAdapter toToolDefinition() {
        return new ToolDefinitionAdapter(
                getToolName(),
                getToolDescription(),
                getParameters(),
                true // skills are generally required in workflows
        );
    }

    /**
     * Tool definition for use with agent framework.
     */
    public record ToolDefinitionAdapter(
        String name,
        String description,
        Map<String, Object> parameters,
        boolean required
    ) {}

    /**
     * Result of tool execution.
     */
    public record ToolExecutionResult(
        String toolName,
        String output,
        boolean success,
        long executionTimeMs,
        String error
    ) {}

    /**
     * Factory method to create adapters for all skills.
     */
    public static Map<String, ManifestSkillToolAdapter> createAdaptersForAllSkills(SkillExecutor executor) throws Exception {
        Map<String, ManifestSkillToolAdapter> adapters = new HashMap<>();

        Map<String, SkillManifest> allSkills = executor.loadAllSkills();
        for (Map.Entry<String, SkillManifest> entry : allSkills.entrySet()) {
            adapters.put(entry.getKey(), new ManifestSkillToolAdapter(entry.getValue(), executor));
        }

        return adapters;
    }
}
