package tech.kayys.gollek.agent.mcp;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.spi.DefaultSkillRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lifecycle for all configured MCP server connections.
 *
 * <p>On startup, reads MCP server configuration from {@code application.yaml}
 * and connects to each declared server. For each server, it calls
 * {@code tools/list} and registers every discovered tool as a
 * {@link McpSkillAdapter} in the {@link DefaultSkillRegistry}.
 *
 * <h2>Configuration</h2>
 * <pre>
 * gollek:
 *   agent:
 *     mcp:
 *       servers:
 *         - id: filesystem
 *           url: http://localhost:3000
 *           enabled: true
 *         - id: github
 *           url: https://mcp.github.com
 *           enabled: true
 *           headers:
 *             Authorization: "Bearer ${GITHUB_TOKEN}"
 *         - id: brave-search
 *           url: http://localhost:3001
 *           enabled: true
 * </pre>
 *
 * <h2>Discovery</h2>
 * Each connected server's tools are prefixed with the server id:
 * {@code filesystem:read_file}, {@code github:list_repos}, etc.
 * This prevents name collisions across servers.
 */
@ApplicationScoped
public class McpServerRegistry {

    private static final Logger LOG = Logger.getLogger(McpServerRegistry.class);

    @Inject Vertx vertx;
    @Inject DefaultSkillRegistry skillRegistry;

    /** Active clients, keyed by server id. */
    private final ConcurrentHashMap<String, McpClient> clients = new ConcurrentHashMap<>();

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
        McpClient client = clients.remove(serverId);
        if (client == null) return Uni.createFrom().voidItem();
        // Unregister all skills from this server
        skillRegistry.listAll().stream()
                .filter(s -> s.id().startsWith(serverId + ":"))
                .forEach(s -> skillRegistry.unregister(s.id()));
        return client.disconnect();
    }

    /** Return all active clients. */
    public Collection<McpClient> activeClients() {
        return Collections.unmodifiableCollection(clients.values());
    }

    /** Get a specific client by id. */
    public Optional<McpClient> getClient(String serverId) {
        return Optional.ofNullable(clients.get(serverId));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Uni<Void> connectAndRegister(McpServerConfig cfg) {
        McpSseClient client = new McpSseClient(vertx, cfg.id(), cfg.url(), cfg.headers());
        return client.connect()
                .chain(() -> client.listTools())
                .invoke(tools -> {
                    clients.put(cfg.id(), client);
                    int registered = 0;
                    for (McpProtocol.McpTool tool : tools) {
                        McpSkillAdapter adapter = new McpSkillAdapter(client, tool);
                        skillRegistry.register(adapter);
                        registered++;
                        LOG.debugf("MCP: registered tool skill '%s'", adapter.id());
                    }
                    LOG.infof("MCP: server '%s' — %d tools registered as skills.", cfg.id(), registered);
                })
                .replaceWithVoid()
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
            if (id.isEmpty() || url.isEmpty()) break;

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
            result.add(new McpServerConfig(id.get(), url.get(), enabled, headers));
            i++;
        }
        return result;
    }

    @PreDestroy
    void onStop() {
        LOG.info("MCP: disconnecting all servers…");
        clients.values().forEach(c -> c.disconnect().subscribe().with(v -> {}, e -> {}));
        clients.clear();
    }

    // ── Config record ──────────────────────────────────────────────────────────

    public record McpServerConfig(
            String id,
            String url,
            boolean enabled,
            Map<String, String> headers
    ) {}
}
