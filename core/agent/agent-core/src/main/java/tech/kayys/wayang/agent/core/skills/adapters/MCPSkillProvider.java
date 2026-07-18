package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(MCPSkillProvider.class);

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
        mcpConfig.put(McpSkillPayloads.KEY_ENDPOINT, endpoint);
        return this;
    }

    /**
     * Configure MCP protocol version.
     */
    public MCPSkillProvider withProtocolVersion(String version) {
        mcpConfig.put(McpSkillPayloads.KEY_PROTOCOL_VERSION, version);
        return this;
    }

    /**
     * Get all skills as MCP resources.
     */
    public Uni<List<MCPSkillResource>> listSkillsAsResources() {
        return Uni.createFrom().item(() ->
            skillRegistry.list()
                .stream()
                .map(McpSkillPayloads::resource)
                .collect(java.util.stream.Collectors.toList())
        );
    }

    /**
     * Execute skill via MCP protocol.
     */
    public Uni<Map<String, Object>> executeViaMCP(String skillId, Map<String, Object> input) {
        return skillRegistry.executeSkill(skillId, input == null ? Map.of() : input)
            .map(result -> McpSkillPayloads.executionResult(skillId, result))
            .onFailure().recoverWithItem(error -> McpSkillPayloads.error(skillId, error));
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
            .invoke(() -> LOG.debugf("MCP skills provider initialized with %d skills", skillRegistry.list().size()));
    }

    /**
     * Get MCP configuration.
     */
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(mcpConfig);
    }
}
