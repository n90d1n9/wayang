package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adapter that exposes skills as tools for the tools module.
 *
 * Enables skills to be discovered and invoked through the tools registry,
 * providing seamless integration between skill orchestration and tool execution.
 */
public class SkillAsToolAdapter implements Tool {

    private final SkillDefinition skill;
    private final SkillRegistry skillRegistry;
    private final SkillMetadata metadata;
    private final Map<String, Object> inputSchema;

    public SkillAsToolAdapter(SkillDefinition skill, SkillRegistry skillRegistry, SkillMetadata metadata) {
        this.skill = Objects.requireNonNull(skill, "skill");
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.inputSchema = SkillToolDescriptors.inputSchema(skill);
    }

    @Override
    public String id() {
        return skill.id();
    }

    @Override
    public String name() {
        return SkillToolDescriptors.toolName(skill, metadata);
    }

    @Override
    public String description() {
        return SkillToolDescriptors.description(metadata);
    }

    @Override
    public Map<String, Object> inputSchema() {
        return inputSchema;
    }

    @Override
    public ToolResult execute(Map<String, Object> input, ToolContext context) {
        try {
            return toToolResult(skillRegistry.executeSkill(skill.id(), safeInput(input)).await().indefinitely());
        } catch (RuntimeException error) {
            return ToolResult.error(SkillResultPayloads.errorMessage(error));
        }
    }

    @Override
    public Uni<ToolResult> executeAsync(Map<String, Object> input, ToolContext context) {
        return skillRegistry.executeSkill(skill.id(), safeInput(input))
                .map(SkillAsToolAdapter::toToolResult)
                .onFailure().recoverWithItem(error -> ToolResult.error(SkillResultPayloads.errorMessage(error)));
    }

    public static List<SkillAsToolAdapter> adaptSkills(SkillRegistry registry) {
        return registry.list()
            .stream()
            .map(skill -> adaptSkill(skill, registry))
            .collect(Collectors.toList());
    }

    public static SkillAsToolAdapter adaptSkill(SkillDefinition skill, SkillRegistry registry) {
        return new SkillAsToolAdapter(skill, registry, SkillToolDescriptors.metadataFrom(skill));
    }

    private static Map<String, Object> safeInput(Map<String, Object> input) {
        return input == null ? Map.of() : input;
    }

    private static ToolResult toToolResult(SkillResult result) {
        if (result == null) {
            return ToolResult.error(SkillResultPayloads.ERROR_NO_RESULT);
        }
        if (!result.success()) {
            return ToolResult.error(SkillResultPayloads.failureMessage(result));
        }
        return ToolResult.success(SkillResultPayloads.resultData(result, SkillResultPayloads.KEY_OBSERVATION));
    }
}
