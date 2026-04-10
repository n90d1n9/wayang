package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * MCP (Model Context Protocol) skill provider.
 * 
 * Exposes unified skills via MCP protocol, enabling remote agents
 * and external systems to discover and invoke skills.
 */
public class MCPSkillProvider {

    private final SkillRegistry skillRegistry;
    private final Map<String, Object> mcpConfig;

    public MCPSkillProvider(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
        this.mcpConfig = new HashMap<>();
    }

    /**
     * Configure MCP endpoint.
     */
    public MCPSkillProvider withEndpoint(String endpoint) {
        mcpConfig.put("endpoint", endpoint);
        return this;
    }

    /**
     * Configure MCP protocol version.
     */
    public MCPSkillProvider withProtocolVersion(String version) {
        mcpConfig.put("protocol_version", version);
        return this;
    }

    /**
     * Get all skills as MCP resources.
     */
    public Uni<List<MCPSkillResource>> listSkillsAsResources() {
        return Uni.createFrom().item(() ->
            skillRegistry.list()
                .stream()
                .map(this::toMCPResource)
                .collect(java.util.stream.Collectors.toList())
        );
    }

    /**
     * Convert skill to MCP resource format.
     */
    private MCPSkillResource toMCPResource(SkillDefinition skill) {
        return new MCPSkillResource(
            skill.id(),
            skill.metadata().name(),
            skill.metadata().description(),
            skill.metadata().version(),
            Map.of(
                "tags", skill.metadata().tags(),
                "category", skill.metadata().category()
            )
        );
    }

    /**
     * Execute skill via MCP protocol.
     */
    public Uni<Map<String, Object>> executeViaMCP(String skillId, Map<String, Object> input) {
        return skillRegistry.executeSkill(skillId, input)
            .map(result -> Map.of(
                "skill_id", skillId,
                "success", result.success(),
                "result", result.observation(),
                "status", result.status().name()
            ));
    }

    /**
     * MCP skill resource representation.
     */
    public record MCPSkillResource(
        String id,
        String name,
        String description,
        String version,
        Map<String, Object> metadata
    ) {}

    /**
     * Initialize MCP provider.
     */
    public Uni<MCPSkillProvider> initialize() {
        return Uni.createFrom().item(this)
            .invoke(() -> System.out.println("[MCP] Skills provider initialized with " +
                skillRegistry.list().size() + " skills"));
    }

    /**
     * Get MCP configuration.
     */
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(mcpConfig);
    }
}
