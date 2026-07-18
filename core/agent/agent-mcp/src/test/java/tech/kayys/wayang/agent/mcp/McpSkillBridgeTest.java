package tech.kayys.wayang.agent.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpSkillBridgeTest {

    @Test
    void registersDiscoveredMcpToolsAsRuntimeSkills() {
        FakeMcpToolClient client = new FakeMcpToolClient(List.of(tool("filesystem", "read_file")));
        SimpleSkillRegistry registry = new SimpleSkillRegistry();
        McpSkillBridge bridge = new McpSkillBridge(client, registry);

        List<McpToolDescriptor> tools = bridge.connect(McpServerConfig.http("filesystem", "http://localhost:3000"))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(tools).extracting(McpToolDescriptor::id).containsExactly("filesystem:read_file");
        assertThat(registry.find("filesystem:read_file")).isPresent();

        Map<String, Object> result = registry.findOrThrow("filesystem:read_file")
                .execute(Map.of("path", "/tmp/demo.txt"))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("skillId", "filesystem:read_file")
                .containsEntry("text", "read /tmp/demo.txt");
        @SuppressWarnings("unchecked")
        Map<Object, Object> resultMetadata = (Map<Object, Object>) result.get("metadata");
        assertThat(resultMetadata).containsEntry("serverId", "filesystem");
        assertThat(client.lastArguments).containsEntry("path", "/tmp/demo.txt");
        assertThat(client.lastInvocation.context())
                .containsEntry(McpTransportContext.KEY_SERVER_ID, "filesystem")
                .containsEntry(McpTransportContext.KEY_TRANSPORT_TYPE, McpTransportType.HTTP.name())
                .containsEntry(McpTransportContext.KEY_MCP_ENDPOINT, "http://localhost:3000");
    }

    @Test
    void reconnectsByReplacingExistingServerSkills() {
        FakeMcpToolClient client = new FakeMcpToolClient(List.of(tool("filesystem", "read_file")));
        SimpleSkillRegistry registry = new SimpleSkillRegistry();
        McpSkillBridge bridge = new McpSkillBridge(client, registry);

        bridge.connect(McpServerConfig.http("filesystem", "http://localhost:3000"))
                .await().atMost(Duration.ofSeconds(2));
        client.tools = List.of(tool("filesystem", "write_file"));
        bridge.connect(McpServerConfig.http("filesystem", "http://localhost:3000"))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(registry.find("filesystem:read_file")).isEmpty();
        assertThat(registry.find("filesystem:write_file")).isPresent();
        assertThat(bridge.activeTools()).extracting(McpToolDescriptor::id)
                .containsExactly("filesystem:write_file");
    }

    @Test
    void disabledServerRemovesPreviouslyRegisteredSkills() {
        FakeMcpToolClient client = new FakeMcpToolClient(List.of(tool("filesystem", "read_file")));
        SimpleSkillRegistry registry = new SimpleSkillRegistry();
        McpSkillBridge bridge = new McpSkillBridge(client, registry);

        bridge.connect(McpServerConfig.http("filesystem", "http://localhost:3000"))
                .await().atMost(Duration.ofSeconds(2));
        bridge.connect(new McpServerConfig(
                "filesystem",
                "http://localhost:3000",
                null,
                List.of(),
                McpTransportType.HTTP,
                false,
                Map.of()))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(registry.find("filesystem:read_file")).isEmpty();
        assertThat(bridge.activeTools()).isEmpty();
    }

    @Test
    void extractsTextFromMcpContentBlocks() {
        McpToolCallResult result = McpToolCallResult.success(Map.of(
                "content", List.of(
                        Map.of("type", "text", "text", "first"),
                        Map.of("type", "text", "text", "second"))), 3);

        assertThat(result.text()).isEqualTo("first\nsecond");
    }

    @Test
    void snapshotsInvocationAndResultPayloads() {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("path", "/tmp/a.txt");
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer token");
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(McpTransportContext.KEY_HEADERS, headers);
        McpToolInvocation invocation = new McpToolInvocation(tool("filesystem", "read_file"), arguments, context);

        arguments.put("path", "/tmp/mutated.txt");
        headers.put("Authorization", "mutated");

        assertThat(invocation.arguments()).containsEntry("path", "/tmp/a.txt");
        @SuppressWarnings("unchecked")
        Map<Object, Object> invocationHeaders =
                (Map<Object, Object>) invocation.context().get(McpTransportContext.KEY_HEADERS);
        assertThat(invocationHeaders)
                .containsEntry("Authorization", "Bearer token");
        assertThatThrownBy(() -> invocation.arguments().put("later", true))
                .isInstanceOf(UnsupportedOperationException.class);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("httpStatus", 200);
        McpToolCallResult result = McpToolCallResult.success(Map.of("text", "ok"), 7, metadata);
        metadata.put("httpStatus", 500);

        assertThat(result.metadata()).containsEntry("httpStatus", 200);
        assertThatThrownBy(() -> result.metadata().put("later", true))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void attachesStdioTransportContextToDiscoveredTools() {
        FakeMcpToolClient client = new FakeMcpToolClient(List.of(tool("ignored", "list_files")));
        SimpleSkillRegistry registry = new SimpleSkillRegistry();
        McpSkillBridge bridge = new McpSkillBridge(client, registry);

        List<McpToolDescriptor> tools = bridge.connect(McpServerConfig.stdio(
                "filesystem",
                "node",
                new ArrayList<>(List.of("server.js"))))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(tools).hasSize(1);
        assertThat(tools.getFirst().serverId()).isEqualTo("filesystem");
        assertThat(tools.getFirst().metadata())
                .containsEntry(McpTransportContext.KEY_COMMAND, "node")
                .containsEntry(McpTransportContext.KEY_TRANSPORT_TYPE, McpTransportType.STDIO.name());
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) tools.getFirst().metadata().get(McpTransportContext.KEY_ARGS);
        assertThat(args)
                .containsExactly("server.js");
    }

    private McpToolDescriptor tool(String serverId, String name) {
        return new McpToolDescriptor(
                serverId,
                name,
                "Tool " + name,
                Map.of("type", "object"),
                Map.of());
    }

    private static final class FakeMcpToolClient implements McpToolClient {
        private List<McpToolDescriptor> tools;
        private Map<String, Object> lastArguments = Map.of();
        private McpToolInvocation lastInvocation;

        private FakeMcpToolClient(List<McpToolDescriptor> tools) {
            this.tools = tools;
        }

        @Override
        public Uni<List<McpToolDescriptor>> listTools(McpServerConfig server) {
            return Uni.createFrom().item(tools);
        }

        @Override
        public Uni<McpToolCallResult> callTool(McpToolInvocation invocation) {
            lastInvocation = invocation;
            lastArguments = invocation.arguments();
            return Uni.createFrom().item(McpToolCallResult.success(
                    Map.of("text", "read " + invocation.arguments().getOrDefault("path", "")),
                    5,
                    Map.of("serverId", invocation.serverId())));
        }
    }

    private static final class SimpleSkillRegistry implements SkillRegistry {
        private final Map<String, AgentSkill> runtimeSkills = new LinkedHashMap<>();
        private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();

        @Override
        public List<AgentSkill> listAll() {
            return List.copyOf(runtimeSkills.values());
        }

        @Override
        public Optional<AgentSkill> find(String id) {
            return Optional.ofNullable(runtimeSkills.get(id));
        }

        @Override
        public AgentSkill findOrThrow(String id) {
            return find(id).orElseThrow();
        }

        @Override
        public void register(AgentSkill skill) {
            runtimeSkills.put(skill.id(), skill);
        }

        @Override
        public void unregister(String skillId) {
            runtimeSkills.remove(skillId);
            definitions.remove(skillId);
        }

        @Override
        public List<AgentSkill> findByCategory(SkillCategory category) {
            return runtimeSkills.values().stream()
                    .filter(skill -> category.name().equalsIgnoreCase(skill.category()))
                    .toList();
        }

        @Override
        public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
            return runtimeSkills.values().stream()
                    .filter(skill -> allowedIds == null || allowedIds.isEmpty() || allowedIds.contains(skill.id()))
                    .toList();
        }

        @Override
        public boolean isRegistered(String skillId) {
            return runtimeSkills.containsKey(skillId) || definitions.containsKey(skillId);
        }

        @Override
        public Map<String, SkillHealth> checkHealth() {
            return Map.of();
        }

        @Override
        public int size() {
            return runtimeSkills.size() + definitions.size();
        }

        @Override
        public Optional<SkillDefinition> getSkill(String skillId) {
            return Optional.ofNullable(definitions.get(skillId));
        }

        @Override
        public List<SkillDefinition> listSkills() {
            return List.copyOf(definitions.values());
        }

        @Override
        public List<SkillDefinition> listByCategory(String category) {
            return definitions.values().stream()
                    .filter(skill -> category.equalsIgnoreCase(skill.category()))
                    .toList();
        }

        @Override
        public void registerSkill(SkillDefinition skill) {
            definitions.put(skill.id(), skill);
        }

        @Override
        public boolean unregisterSkill(String skillId) {
            return definitions.remove(skillId) != null;
        }
    }
}
