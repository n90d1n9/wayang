package tech.kayys.gollek.agent.mcp;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.spi.SkillCategory;
import tech.kayys.gollek.agent.spi.SkillContext;
import tech.kayys.gollek.agent.spi.SkillResult;
import tech.kayys.gollek.agent.spi.AgentSkill;
import tech.kayys.gollek.mcp.client.MCPConnection;
import tech.kayys.gollek.mcp.dto.MCPTool;
import tech.kayys.gollek.mcp.dto.MCPResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapts a single MCP tool into a Gollek {@link AgentSkill}.
 *
 * <p>When the {@link McpServerRegistry} connects to an MCP server, it discovers
 * all available tools via {@code tools/list} and creates one {@code McpSkillAdapter}
 * per tool. These adapters are then registered into the {@link DefaultSkillRegistry}
 * so they are available to all orchestrators without any additional configuration.</p>
 *
 * <h2>Tool schema</h2>
 * The adapter also implements {@link NativeToolCallingOrchestrator.ToolSchemaProvider}
 * so that the native tool-calling orchestrator can send accurate JSON schemas to the
 * LLM — enabling proper structured function calling for MCP-sourced tools.
 */
public class McpSkillAdapter implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(McpSkillAdapter.class);

    private final MCPConnection connection;
    private final MCPTool tool;
    private final String serverId;
    private final String qualifiedId;   // "{serverId}:{toolName}"

    public McpSkillAdapter(MCPConnection connection, MCPTool tool, String serverId) {
        this.connection = connection;
        this.tool = tool;
        this.serverId = serverId;
        // Qualify with server id to avoid collisions across servers
        this.qualifiedId = serverId + ":" + tool.getName();
    }

    // ── AgentSkill identity ────────────────────────────────────────────────────

    @Override public String id()          { return qualifiedId; }
    @Override public String name()        { return tool.getName() + " (MCP/" + connection.getConfig().getName() + ")"; }
    @Override public String description() { return tool.getDescription() != null ? tool.getDescription() : tool.getName(); }
    @Override public String version()     { return "1.0.0"; }
    @Override public SkillCategory category() { return SkillCategory.SYSTEM; }
    @Override public boolean isHealthy()  { return connection.isConnected(); }

    // ── Execution ─────────────────────────────────────────────────────────────

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        long t0 = System.currentTimeMillis();

        if (!connection.isConnected()) {
            return Uni.createFrom().item(SkillResult.failure(
                    id(), "MCP server '" + serverId + "' is not connected."));
        }

        // The inputs from SkillContext are the arguments to pass through to MCP
        Map<String, Object> args = ctx.getInputs();

        LOG.debugf("MCP tool call: server=%s tool=%s args=%s", serverId, tool.getName(), args);

        return connection.callTool(tool.getName(), args)
                .map(response -> {
                    long dur = System.currentTimeMillis() - t0;
                    if (!response.isSuccess()) {
                        return SkillResult.builder()
                                .skillId(id())
                                .status(SkillResult.Status.FAILURE)
                                .observation("MCP tool error: " + response.getError())
                                .durationMs(dur)
                                .build();
                    }
                    
                    // Parse the response result
                    String text = extractTextFromResponse(response);
                    return SkillResult.builder()
                            .skillId(id())
                            .status(SkillResult.Status.SUCCESS)
                            .observation(text)
                            .output("text", text)
                            .output("raw_response", response.getResult())
                            .durationMs(dur)
                            .build();
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("MCP tool call failed: %s", err.getMessage());
                    return SkillResult.failure(id(), err.getMessage());
                });
    }

    /**
     * Extract text content from MCP response.
     * Handles both old-style map responses and new content block responses.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(MCPResponse response) {
        if (response.getResult() == null) return "";
        
        if (response.getResult() instanceof Map<?, ?> resultMap) {
            // Check for content array (new format)
            if (resultMap.containsKey("content")) {
                Object contentObj = resultMap.get("content");
                if (contentObj instanceof List<?> contentList) {
                    return contentList.stream()
                            .filter(item -> item instanceof Map)
                            .map(item -> (Map<String, Object>) item)
                            .filter(block -> "text".equals(block.get("type")))
                            .map(block -> (String) block.get("text"))
                            .filter(text -> text != null)
                            .collect(Collectors.joining("\n"));
                }
            }
            
            // Check for text field (simple format)
            if (resultMap.containsKey("text")) {
                return resultMap.get("text").toString();
            }
            
            // Fallback: convert entire result to string
            return resultMap.toString();
        }
        
        return response.getResult().toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "McpSkillAdapter{server=" + serverId + ", tool=" + tool.getName() + "}";
    }
}
