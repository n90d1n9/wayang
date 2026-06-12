package tech.kayys.gollek.agent.mcp;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.spi.DefaultSkillRegistry;
import tech.kayys.gollek.mcp.client.MCPClient;
import tech.kayys.gollek.mcp.client.MCPClientConfig;
import tech.kayys.gollek.mcp.dto.MCPConnection;
import tech.kayys.gollek.mcp.dto.MCPTool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lifecycle for all configured MCP server connections.
 *
 * <p>On startup, reads MCP server configuration from {@code application.yaml}
 * and connects to each declared server via the MCP plugin's {@link MCPClient}.
 * For each server, it calls {@code tools/list} and registers every discovered 
 * tool as a {@link McpSkillAdapter} in the {@link DefaultSkillRegistry}.
 *
 * <h2>Configuration</h2>
 * <pre>
 * gollek:
 *   agent:
 *     mcp:
 *       servers:
 *         - id: filesystem
 *           url: http://localhost:3000
 *           transport: HTTP
 *           enabled: true
 *         - id: code-executor
 *           command: python
 *           args: ["-m", "mcp_server"]
 *           transport: STDIO
 *           enabled: true
 * </pre>
 *
 * <h2>Discovery</h2>
 * Each connected server's tools are prefixed with the server id:
 * {@code filesystem:read_file}, {@code code-executor:execute}, etc.
 * This prevents name collisions across servers.
 */
@ApplicationScoped
public class McpServerRegistry {

    private static final Logger LOG = Logger.getLogger(McpServerRegistry.class);

    @Inject MCPClient mcpClient;
    @Inject DefaultSkillRegistry skillRegistry;

    /** Active connections, keyed by server id. */
    private final ConcurrentHashMap<String, MCPConnection> connections = new ConcurrentHashMap<>();

    // ── Startup ───────────────────────────────────────────────────────────────

    void onStart(@Observes StartupEvent event) {
        List<McpServerConfig> configs = readConfig();
        if (configs.isEmpty()) {
            LOG.info("MCP: no servers configured. Add gollek.agent.mcp.servers to enable MCP.");
            return;
        }
        LOG.infof("MCP: connecting to %d server(s)…", configs.size());
        configs.stream()
                .filter(McpServerConfig::enabled)
                .forEach(cfg -> connectAndRegister(cfg)
                        .subscribe().with(
                                v -> LOG.infof("MCP: server '%s' connected and tools registered.", cfg.id()),
                                e -> LOG.warnf("MCP: server '%s' connection failed: %s — continuing without it.",
                                        cfg.id(), e.getMessage())));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Connect to a new MCP server at runtime and register its tools. */
    public Uni<Void> addServer(McpServerConfig config) {
        return connectAndRegister(config);
    }

    /** Remove a server and unregister its tools. */
    public Uni<Void> removeServer(String serverId) {
        MCPConnection connection = connections.remove(serverId);
        if (connection == null) return Uni.createFrom().voidItem();
        // Unregister all skills from this server
        skillRegistry.listAll().stream()
                .filter(s -> s.id().startsWith(serverId + ":"))
                .forEach(s -> skillRegistry.unregister(s.id()));
        return mcpClient.disconnect(serverId);
    }

    /** Return all active connections. */
    public Collection<MCPConnection> activeConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /** Get a specific connection by id. */
    public Optional<MCPConnection> getConnection(String serverId) {
        return Optional.ofNullable(connections.get(serverId));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Uni<Void> connectAndRegister(McpServerConfig cfg) {
        MCPClientConfig config = MCPClientConfig.builder()
                .name(cfg.id())
                .url(cfg.url())
                .command(cfg.command())
                .args(cfg.args())
                .transportType(cfg.transportType())
                .headers(cfg.headers())
                .build();

        return mcpClient.connect(config)
                .onItem().transformToUni(connection -> {
                    connections.put(cfg.id(), connection);
                    Map<String, MCPTool> tools = connection.getTools();
                    int registered = 0;
                    for (MCPTool tool : tools.values()) {
                        McpSkillAdapter adapter = new McpSkillAdapter(connection, tool, cfg.id());
                        try {
                            skillRegistry.register(adapter);
                            registered++;
                            LOG.debugf("MCP: registered tool skill '%s'", adapter.id());
                        } catch (Exception e) {
                            LOG.warnf("MCP: failed to register tool '%s': %s", tool.getName(), e.getMessage());
                        }
                    }
                    LOG.infof("MCP: server '%s' — %d tools registered as skills.", cfg.id(), registered);
                    return Uni.createFrom().voidItem();
                })
                .onFailure().invoke(e ->
                        LOG.warnf("MCP: failed to connect/register server '%s': %s", cfg.id(), e.getMessage()));
    }

    /** Read MCP server configs from MicroProfile Config. */
    private List<McpServerConfig> readConfig() {
        Config cfg = ConfigProvider.getConfig();
        List<McpServerConfig> result = new ArrayList<>();
        int i = 0;
        while (true) {
            String prefix = "gollek.agent.mcp.servers[" + i + "]";
            Optional<String> id  = cfg.getOptionalValue(prefix + ".id",  String.class);
            Optional<String> url = cfg.getOptionalValue(prefix + ".url", String.class);
            Optional<String> command = cfg.getOptionalValue(prefix + ".command", String.class);
            
            if ((id.isEmpty() && command.isEmpty()) || (url.isEmpty() && command.isEmpty())) break;

            String transportStr = cfg.getOptionalValue(prefix + ".transport", String.class).orElse("HTTP");
            boolean enabled = cfg.getOptionalValue(prefix + ".enabled", Boolean.class).orElse(true);
            Map<String, String> headers = new LinkedHashMap<>();
            
            // Read up to 10 extra headers
            for (int h = 0; h < 10; h++) {
                String hPrefix = prefix + ".headers[" + h + "]";
                Optional<String> hName  = cfg.getOptionalValue(hPrefix + ".name",  String.class);
                Optional<String> hValue = cfg.getOptionalValue(hPrefix + ".value", String.class);
                if (hName.isEmpty()) break;
                headers.put(hName.get(), hValue.orElse(""));
            }
            
            // Read args
            List<String> args = new ArrayList<>();
            int a = 0;
            while (true) {
                Optional<String> arg = cfg.getOptionalValue(prefix + ".args[" + a + "]", String.class);
                if (arg.isEmpty()) break;
                args.add(arg.get());
                a++;
            }
            
            result.add(new McpServerConfig(
                    id.orElse(null), 
                    url.orElse(null), 
                    command.orElse(null),
                    args,
                    transportStr,
                    enabled, 
                    headers));
            i++;
        }
        return result;
    }

    @PreDestroy
    void onStop() {
        LOG.info("MCP: disconnecting all servers…");
        connections.keySet().forEach(serverId -> {
            mcpClient.disconnect(serverId).subscribe().with(v -> {}, e -> {});
        });
        connections.clear();
    }

    // ── Config record ──────────────────────────────────────────────────────────

    public record McpServerConfig(
            String id,
            String url,
            String command,
            List<String> args,
            String transportType,
            boolean enabled,
            Map<String, String> headers
    ) {
        public tech.kayys.gollek.mcp.client.MCPClientConfig.TransportType transportType() {
            try {
                return tech.kayys.gollek.mcp.client.MCPClientConfig.TransportType.valueOf(transportType);
            } catch (Exception e) {
                return tech.kayys.gollek.mcp.client.MCPClientConfig.TransportType.HTTP;
            }
        }
    }
}
