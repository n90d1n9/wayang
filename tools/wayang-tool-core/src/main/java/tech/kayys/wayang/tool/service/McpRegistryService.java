package tech.kayys.wayang.tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.McpRegistryImportResponse;
import tech.kayys.wayang.tool.dto.McpServerConfigRequest;
import tech.kayys.wayang.tool.dto.McpServerRegistryResponse;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class McpRegistryService {

    @Inject
    McpServerRegistryRepository repository;

    @Inject
    EditionModeService editionModeService;

    private final ObjectMapper mapper = new ObjectMapper();

    public Uni<McpRegistryImportResponse> importFromJson(String requestId, McpRegistryImportRequest request) {
        if (!editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                    "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
        }
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            try {
                if (request.source() == null || request.source().isBlank()) {
                    return Uni.createFrom().failure(new IllegalArgumentException("`source` must not be empty."));
                }
                String content = loadContent(request.sourceType(), request.source());
                JsonNode root = mapper.readTree(content);
                JsonNode serversNode = root.has("mcpServers") ? root.path("mcpServers") : root;
                if (!serversNode.isObject()) {
                    return Uni.createFrom().failure(new IllegalArgumentException("MCP JSON must be an object or contain `mcpServers` object."));
                }

                List<Uni<String>> writes = new ArrayList<>();
                serversNode.fields().forEachRemaining(entry -> {
                    String serverName = entry.getKey();
                    if (request.serverName() != null && !request.serverName().isBlank() && !request.serverName().equals(serverName)) {
                        return;
                    }

                    JsonNode server = entry.getValue();
                    String transport = resolveTransport(server);
                    String command = text(server, "command");
                    String url = text(server, "url");
                    String argsJson = server.has("args") ? server.path("args").toString() : null;
                    String envJson = server.has("env") ? server.path("env").toString() : null;
                    boolean enabled = !server.has("enabled") || server.path("enabled").asBoolean(true);
                    validateServer(serverName, transport, command, url);

                    writes.add(upsertServerEntity(requestId, serverName, transport, command, url, argsJson, envJson, enabled, request.source(), null));
                });

                if (writes.isEmpty()) {
                    return Uni.createFrom().item(new McpRegistryImportResponse(0, List.of()));
                }

                Uni<List<String>> chain = Uni.createFrom().item(new ArrayList<String>());
                for (Uni<String> write : writes) {
                    chain = chain.flatMap(names -> write.map(name -> {
                        names.add(name);
                        return names;
                    }));
                }

                return chain.map(names -> new McpRegistryImportResponse(names.size(), names.stream().sorted().toList()));
            } catch (Exception e) {
                return Uni.createFrom().failure(new IllegalArgumentException("Failed to import MCP JSON: " + e.getMessage(), e));
            }
        });
    }

    public Uni<List<McpServerRegistryResponse>> listServers(String requestId) {
        if (!editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                    "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
        }
        return repository.listByRequestId(requestId)
                .map(items -> items.stream()
                        .sorted(Comparator.comparing(McpServerRegistry::getName))
                        .map(item -> new McpServerRegistryResponse(
                                item.getName(),
                                item.getTransport(),
                                item.getCommand(),
                                item.getUrl(),
                                item.getArgsJson(),
                                item.getEnvJson(),
                                item.getSource(),
                                item.getSyncSchedule(),
                                item.isEnabled()))
                        .toList());
    }

    public Uni<McpServerRegistryResponse> upsertServer(String requestId, String name, McpServerConfigRequest request) {
        if (!editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                    "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
        }
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            try {
                String transport = resolveTransport(request.transport(), request.url());
                validateServer(name, transport, request.command(), request.url());
                String argsJson = request.args() == null ? null : mapper.writeValueAsString(request.args());
                String envJson = request.env() == null ? null : mapper.writeValueAsString(request.env());
                boolean enabled = request.enabled() == null || request.enabled();
                String source = request.source() == null || request.source().isBlank() ? "manual://api" : request.source();
                String syncSchedule = normalizeSchedule(request.syncSchedule());

                return upsertServerEntity(
                        requestId,
                        name,
                        transport,
                        request.command(),
                        request.url(),
                        argsJson,
                        envJson,
                        enabled,
                        source,
                        syncSchedule)
                        .flatMap(serverName -> repository.findByRequestIdAndName(requestId, serverName))
                        .map(item -> new McpServerRegistryResponse(
                                item.getName(),
                                item.getTransport(),
                                item.getCommand(),
                                item.getUrl(),
                                item.getArgsJson(),
                                item.getEnvJson(),
                                item.getSource(),
                                item.getSyncSchedule(),
                                item.isEnabled()));
            } catch (JsonProcessingException e) {
                return Uni.createFrom().failure(new IllegalArgumentException("Failed to serialize MCP server payload: " + e.getMessage(), e));
            }
        });
    }

    public Uni<Boolean> removeServer(String requestId, String name) {
        if (!editionModeService.supportsMcpRegistryDatabase()) {
            return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                    "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
        }
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(
                () -> repository.deleteByRequestIdAndName(requestId, name));
    }

    private Uni<String> upsertServerEntity(
            String requestId,
            String serverName,
            String transport,
            String command,
            String url,
            String argsJson,
            String envJson,
            boolean enabled,
            String source,
            String syncSchedule) {
        return repository.findByRequestIdAndName(requestId, serverName)
                .flatMap(existing -> {
                    McpServerRegistry entity = existing != null ? existing : new McpServerRegistry();
                    Instant now = Instant.now();
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(now);
                    }
                    entity.setUpdatedAt(now);
                    entity.setLastSyncAt(now);
                    entity.setRequestId(requestId);
                    entity.setName(serverName);
                    entity.setTransport(transport);
                    entity.setCommand(command);
                    entity.setUrl(url);
                    entity.setArgsJson(argsJson);
                    entity.setEnvJson(envJson);
                    entity.setEnabled(enabled);
                    entity.setSource(source);
                    entity.setSyncSchedule(syncSchedule);
                    return repository.save(entity).map(saved -> saved.getName());
                });
    }

    private String loadContent(String sourceTypeRaw, String source) throws IOException, InterruptedException {
        String sourceType = sourceTypeRaw == null || sourceTypeRaw.isBlank() ? "RAW" : sourceTypeRaw.trim().toUpperCase(Locale.ROOT);
        return switch (sourceType) {
            case "URL" -> fetch(source);
            case "FILE" -> Files.readString(Path.of(source), StandardCharsets.UTF_8);
            case "RAW" -> source;
            default -> throw new IllegalArgumentException("Unsupported sourceType: " + sourceType + " (allowed: RAW, FILE, URL)");
        };
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String resolveTransport(JsonNode server) {
        String transport = text(server, "transport");
        if (transport != null && !transport.isBlank()) {
            return transport.toLowerCase(Locale.ROOT);
        }
        String type = text(server, "type");
        if (type != null && !type.isBlank()) {
            return type.toLowerCase(Locale.ROOT);
        }
        String url = text(server, "url");
        if (url != null && (url.startsWith("ws://") || url.startsWith("wss://"))) {
            return "websocket";
        }
        if (url != null && !url.isBlank()) {
            return "http";
        }
        return "stdio";
    }

    private String resolveTransport(String transport, String url) {
        if (transport != null && !transport.isBlank()) {
            return transport.trim().toLowerCase(Locale.ROOT);
        }
        if (url != null && (url.startsWith("ws://") || url.startsWith("wss://"))) {
            return "websocket";
        }
        if (url != null && !url.isBlank()) {
            return "http";
        }
        return "stdio";
    }

    private void validateServer(String name, String transport, String command, String url) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Server name must not be blank.");
        }
        String normalized = transport == null ? "stdio" : transport.trim().toLowerCase(Locale.ROOT);
        if ("stdio".equals(normalized) && (command == null || command.isBlank())) {
            throw new IllegalArgumentException("MCP server `" + name + "` with stdio transport requires `command`.");
        }
        if (!"stdio".equals(normalized) && (url == null || url.isBlank())) {
            throw new IllegalArgumentException("MCP server `" + name + "` with `" + normalized + "` transport requires `url`.");
        }
    }

    private String normalizeSchedule(String syncSchedule) {
        if (syncSchedule == null || syncSchedule.isBlank()) {
            return null;
        }
        RegistrySyncService.parseInterval(syncSchedule);
        return syncSchedule.trim();
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node.path(key);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
