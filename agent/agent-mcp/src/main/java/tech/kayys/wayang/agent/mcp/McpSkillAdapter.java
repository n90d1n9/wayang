package tech.kayys.wayang.agent.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpSkillAdapter implements AgentSkill {

    private final McpToolDescriptor tool;
    private final McpToolClient client;

    public McpSkillAdapter(McpToolDescriptor tool, McpToolClient client) {
        this.tool = Objects.requireNonNull(tool, "tool");
        this.client = Objects.requireNonNull(client, "client");
    }

    public McpToolDescriptor tool() {
        return tool;
    }

    @Override
    public String id() {
        return tool.id();
    }

    @Override
    public String name() {
        return tool.name() + " (MCP/" + tool.serverId() + ")";
    }

    @Override
    public String description() {
        return tool.description();
    }

    @Override
    public String category() {
        return SkillCategory.SYSTEM.name();
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        long startedAt = System.currentTimeMillis();
        Map<String, Object> arguments = context == null ? Map.of() : context;
        return client.callTool(McpToolInvocation.of(tool, arguments))
                .map(result -> {
                    long durationMs = result.durationMs() > 0
                            ? result.durationMs()
                            : System.currentTimeMillis() - startedAt;
                    if (!result.success()) {
                        return failureOutput(result.error(), durationMs, result.metadata());
                    }
                    return successOutput(result, durationMs);
                })
                .onFailure().recoverWithItem(error -> failureOutput(
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                        System.currentTimeMillis() - startedAt,
                        Map.of()));
    }

    private Map<String, Object> successOutput(McpToolCallResult result, long durationMs) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("success", true);
        output.put("skillId", id());
        output.put("text", result.text());
        output.put("durationMs", durationMs);
        if (result.result() != null) {
            output.put("rawResponse", result.result());
        }
        if (!result.metadata().isEmpty()) {
            output.put("metadata", result.metadata());
        }
        return Map.copyOf(output);
    }

    private Map<String, Object> failureOutput(String error, long durationMs, Map<String, Object> metadata) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("success", false);
        output.put("skillId", id());
        output.put("error", error == null ? "MCP tool call failed" : error);
        output.put("durationMs", durationMs);
        if (metadata != null && !metadata.isEmpty()) {
            output.put("metadata", McpMaps.copy(metadata));
        }
        return Map.copyOf(output);
    }
}
