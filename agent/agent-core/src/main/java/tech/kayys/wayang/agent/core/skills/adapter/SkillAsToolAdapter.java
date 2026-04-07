package tech.kayys.wayang.agent.core.skills.adapter;

import tech.kayys.wayang.agent.core.skills.SkillsLoader.SkillMetadata;
import tech.kayys.wayang.agent.core.skills.executor.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.executor.SkillExecutor.SkillExecutionResult;

import java.util.*;

/**
 * Adapts a skill to work as a tool in the agent framework.
 * Bridges the gap between skills and agent tool invocations.
 */
public class SkillAsToolAdapter {

    private final SkillMetadata metadata;
    private final SkillExecutor executor;

    public SkillAsToolAdapter(SkillMetadata metadata, SkillExecutor executor) {
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
        String allowedTools = metadata.getAllowedTools();
        if (allowedTools == null || allowedTools.isEmpty()) {
            return Map.of();
        }
        // Parse JSON string to Map (allowedTools is JSON string)
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(allowedTools, java.util.HashMap.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Execute the skill as a tool.
     */
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();

        try {
            SkillExecutionResult result = executor.executeSkill(metadata.getName(), parameters);

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
    public static Map<String, SkillAsToolAdapter> createAdaptersForAllSkills(SkillExecutor executor) throws Exception {
        Map<String, SkillAsToolAdapter> adapters = new HashMap<>();

        Map<String, SkillMetadata> allSkills = executor.loadAllSkills();
        for (Map.Entry<String, SkillMetadata> entry : allSkills.entrySet()) {
            adapters.put(entry.getKey(), new SkillAsToolAdapter(entry.getValue(), executor));
        }

        return adapters;
    }
}
