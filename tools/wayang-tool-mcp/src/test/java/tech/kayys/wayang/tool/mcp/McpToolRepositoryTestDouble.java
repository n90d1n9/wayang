package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class McpToolRepositoryTestDouble implements ToolRepository {
    private final List<McpTool> tools;

    McpToolRepositoryTestDouble() {
        this(List.of());
    }

    McpToolRepositoryTestDouble(List<McpTool> tools) {
        this.tools = new ArrayList<>(tools);
    }

    List<McpTool> tools() {
        return tools;
    }

    void add(McpTool tool) {
        tools.add(tool);
    }

    void addAll(List<McpTool> tools) {
        this.tools.addAll(tools);
    }

    McpTool findLocal(String toolId) {
        return tools.stream()
                .filter(tool -> Objects.equals(toolId, tool.getToolId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Uni<List<McpTool>> listAllTools() {
        return Uni.createFrom().item(List.copyOf(tools));
    }

    @Override
    public Uni<List<McpTool>> findByRequestId(String requestId) {
        return Uni.createFrom().item(tools.stream()
                .filter(tool -> Objects.equals(requestId, tool.getRequestId()))
                .toList());
    }

    @Override
    public Uni<List<McpTool>> findByNamespace(String namespace) {
        return Uni.createFrom().item(tools.stream()
                .filter(tool -> Objects.equals(namespace, tool.getNamespace()))
                .toList());
    }

    @Override
    public Uni<List<McpTool>> findByRequestIdAndNamespace(
            String requestId,
            String namespace) {
        return Uni.createFrom().item(tools.stream()
                .filter(tool -> Objects.equals(requestId, tool.getRequestId()))
                .filter(tool -> Objects.equals(namespace, tool.getNamespace()))
                .toList());
    }

    @Override
    public Uni<McpTool> findById(String toolId) {
        return Uni.createFrom().item(findLocal(toolId));
    }

    @Override
    public Uni<McpTool> findByRequestIdAndToolId(String requestId, String toolId) {
        return Uni.createFrom().item(tools.stream()
                .filter(tool -> Objects.equals(requestId, tool.getRequestId()))
                .filter(tool -> Objects.equals(toolId, tool.getToolId()))
                .findFirst()
                .orElse(null));
    }

    @Override
    public Uni<McpTool> save(McpTool tool) {
        tools.add(tool);
        return Uni.createFrom().item(tool);
    }

    @Override
    public Uni<McpTool> update(McpTool tool) {
        if (!tools.contains(tool)) {
            tools.add(tool);
        }
        return Uni.createFrom().item(tool);
    }

    @Override
    public Uni<Boolean> deleteById(String toolId) {
        return Uni.createFrom().item(tools.removeIf(tool -> Objects.equals(toolId, tool.getToolId())));
    }

    @Override
    public Uni<List<McpTool>> searchTools(String query, Object... params) {
        return listAllTools();
    }

    @Override
    public Uni<Long> count() {
        return Uni.createFrom().item((long) tools.size());
    }

    @Override
    public Uni<Long> countByRequestId(String requestId) {
        return findByRequestId(requestId).map(List::size).map(Integer::longValue);
    }
}
