package tech.kayys.wayang.agent.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpSkillBridge {

    private final McpToolClient client;
    private final SkillRegistry skillRegistry;
    private final Map<String, List<McpToolDescriptor>> toolsByServer = new LinkedHashMap<>();

    public McpSkillBridge(McpToolClient client, SkillRegistry skillRegistry) {
        this.client = Objects.requireNonNull(client, "client");
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
    }

    public Uni<List<McpToolDescriptor>> connect(McpServerConfig server) {
        if (!server.enabled()) {
            unregisterServerSkills(server.id());
            toolsByServer.remove(server.id());
            return Uni.createFrom().item(List.of());
        }
        return client.listTools(server)
                .map(tools -> {
                    List<McpToolDescriptor> normalized = tools.stream()
                            .map(tool -> withServerContext(server, tool))
                            .toList();
                    unregisterServerSkills(server.id());
                    normalized.forEach(tool -> skillRegistry.register(new McpSkillAdapter(tool, client)));
                    toolsByServer.put(server.id(), normalized);
                    return normalized;
                });
    }

    public Uni<Void> disconnect(String serverId) {
        unregisterServerSkills(serverId);
        toolsByServer.remove(serverId);
        return client.disconnect(serverId);
    }

    public List<McpToolDescriptor> activeTools() {
        return toolsByServer.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private McpToolDescriptor withServerContext(McpServerConfig server, McpToolDescriptor tool) {
        return new McpToolDescriptor(
                server.id(),
                tool.name(),
                tool.description(),
                tool.inputSchema(),
                McpTransportContext.merge(server, tool.metadata()));
    }

    private void unregisterServerSkills(String serverId) {
        List<McpToolDescriptor> previous = toolsByServer.getOrDefault(serverId, List.of());
        previous.forEach(tool -> skillRegistry.unregister(tool.id()));
    }
}
