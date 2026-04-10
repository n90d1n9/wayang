package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolDescriptor;
import tech.kayys.wayang.tools.spi.ToolSchema;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;
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

    public SkillAsToolAdapter(SkillDefinition skill, SkillRegistry skillRegistry, SkillMetadata metadata) {
        this.skill = skill;
        this.skillRegistry = skillRegistry;
        this.metadata = metadata;
    }

    @Override
    public String getName() {
        return skill.id();
    }

    @Override
    public String getDescription() {
        return metadata.description();
    }

    @Override
    public ToolDescriptor getDescriptor() {
        return new ToolDescriptor(
            skill.id(),
            metadata.description(),
            createSchema(),
            metadata.version(),
            List.copyOf(metadata.tags())
        );
    }

    @Override
    public ToolSchema getSchema() {
        return createSchema();
    }

    private ToolSchema createSchema() {
        return new ToolSchema(
            skill.id(),
            metadata.description(),
            Map.of(
                "skillId", "string",
                "skillVersion", "string",
                "skillTags", "array"
            ),
            List.of("skillId")
        );
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> input) {
        return skillRegistry.executeSkill(skill.id(), input)
            .map(result -> Map.of(
                "success", result.success(),
                "observation", result.observation(),
                "status", result.status().name()
            ));
    }

    public static List<SkillAsToolAdapter> adaptSkills(SkillRegistry registry) {
        return registry.list()
            .stream()
            .map(skill -> new SkillAsToolAdapter(skill, registry, skill.metadata()))
            .collect(Collectors.toList());
    }
}
