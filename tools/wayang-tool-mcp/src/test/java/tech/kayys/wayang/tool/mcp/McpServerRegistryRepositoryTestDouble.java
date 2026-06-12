package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class McpServerRegistryRepositoryTestDouble implements McpServerRegistryRepository {
    private final List<McpServerRegistry> servers;
    private int findByRequestIdAndNameCalls;

    McpServerRegistryRepositoryTestDouble() {
        this(List.of());
    }

    McpServerRegistryRepositoryTestDouble(List<McpServerRegistry> servers) {
        this.servers = new ArrayList<>(servers);
    }

    List<McpServerRegistry> servers() {
        return servers;
    }

    void add(McpServerRegistry server) {
        servers.add(server);
    }

    void addAll(List<McpServerRegistry> servers) {
        this.servers.addAll(servers);
    }

    int findByRequestIdAndNameCalls() {
        return findByRequestIdAndNameCalls;
    }

    @Override
    public Uni<McpServerRegistry> findByRequestIdAndName(String requestId, String name) {
        findByRequestIdAndNameCalls++;
        return Uni.createFrom().item(servers.stream()
                .filter(server -> Objects.equals(requestId, server.getRequestId()))
                .filter(server -> Objects.equals(name, server.getName()))
                .findFirst()
                .orElse(null));
    }

    @Override
    public Uni<List<McpServerRegistry>> listByRequestId(String requestId) {
        return Uni.createFrom().item(servers.stream()
                .filter(server -> Objects.equals(requestId, server.getRequestId()))
                .toList());
    }

    @Override
    public Uni<List<McpServerRegistry>> listScheduledCandidates() {
        return Uni.createFrom().item(List.copyOf(servers));
    }

    @Override
    public Uni<McpServerRegistry> save(McpServerRegistry entity) {
        if (!servers.contains(entity)) {
            servers.add(entity);
        }
        return Uni.createFrom().item(entity);
    }

    @Override
    public Uni<McpServerRegistry> update(McpServerRegistry entity) {
        if (!servers.contains(entity)) {
            servers.add(entity);
        }
        return Uni.createFrom().item(entity);
    }

    @Override
    public Uni<Boolean> deleteByRequestIdAndName(String requestId, String name) {
        return Uni.createFrom().item(servers.removeIf(server ->
                Objects.equals(requestId, server.getRequestId()) && Objects.equals(name, server.getName())));
    }
}
