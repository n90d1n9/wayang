package tech.kayys.gollek.agent.mcp;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.spi.SkillCategory;
import tech.kayys.gollek.agent.spi.SkillContext;
import tech.kayys.gollek.agent.spi.SkillResult;
import tech.kayys.gollek.agent.spi.AgentSkill;

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

    private final McpClient client;
    private final McpProtocol.McpTool tool;
    private final String qualifiedId;   // "{serverId}:{toolName}"

    public McpSkillAdapter(McpClient client, McpProtocol.McpTool tool) {
        this.client      = client;
        this.tool        = tool;
        // Qualify with server id to avoid collisions across servers
        this.qualifiedId = client.serverId() + ":" + tool.name();
    }

    // ── AgentSkill identity ────────────────────────────────────────────────────

    @Override public String id()          { return qualifiedId; }
    @Override public String name()        { return tool.name() + " (MCP/" + client.serverName() + ")"; }
    @Override public String description() { return tool.description() != null ? tool.description() : tool.name(); }
    @Override public String version()     { return "1.0.0"; }
    @Override public SkillCategory category() { return SkillCategory.SYSTEM; }
    @Override public boolean isHealthy()  { return client.isConnected(); }

    // ── Execution ─────────────────────────────────────────────────────────────

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        long t0 = System.currentTimeMillis();

        if (!client.isConnected()) {
            return Uni.createFrom().item(SkillResult.failure(
                    id(), "MCP server '" + client.serverId() + "' is not connected."));
        }

        // The inputs from SkillContext are the arguments to pass through to MCP
        Map<String, Object> args = ctx.getInputs();

        LOG.debugf("MCP tool call: server=%s tool=%s args=%s", client.serverId(), tool.name(), args);

        return client.callTool(tool.name(), args)
                .map(result -> {
                    long dur = System.currentTimeMillis() - t0;
                    if (result.failed()) {
                        return SkillResult.builder()
                                .skillId(id())
                                .status(SkillResult.Status.FAILURE)
                                .observation("MCP tool error: " + result.allText())
                                .durationMs(dur)
                                .build();
                    }
                    String text = result.allText();
                    return SkillResult.builder()
                            .skillId(id())
                            .status(SkillResult.Status.SUCCESS)
                            .observation(text)
                            .output("text", text)
                            .output("content_blocks", result.content())
                            .durationMs(dur)
                            .build();
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("MCP tool call failed: %s", err.getMessage());
                    return SkillResult.failure(id(), err.getMessage());
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "McpSkillAdapter{server=" + client.serverId() + ", tool=" + tool.name() + "}";
    }
}
