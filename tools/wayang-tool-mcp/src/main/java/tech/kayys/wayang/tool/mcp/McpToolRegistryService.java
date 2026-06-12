package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.util.List;

@ApplicationScoped
public class McpToolRegistryService {

    @Inject
    ToolRepository toolRepository;

    @Inject
    McpToolClient toolClient;

    Uni<List<McpTool>> listAvailableTools(String requestId, McpToolRegistryFilters filters) {
        return filteredRegistryTools(requestId, filters)
                .map(items -> items.stream()
                        .map(tool -> McpToolRegistryMapper.toMcpTool(tool, toolClient))
                        .toList());
    }

    Uni<List<McpToolRegistryEntry>> listRegistryEntries(String requestId, McpToolRegistryFilters filters) {
        return filteredRegistryTools(requestId, filters)
                .map(items -> items.stream()
                        .map(McpToolRegistryMapper::toRegistryEntry)
                        .toList());
    }

    Uni<McpToolRegistrySummary> summarizeRegistry(String requestId, McpToolRegistryFilters filters) {
        return filteredRegistryTools(requestId, filters)
                .map(McpToolRegistrySummaries::from);
    }

    Uni<McpToolRegistryEntry> findRegistryEntry(String requestId, String toolId) {
        return toolRepository.findByRequestIdAndToolId(requestId, toolId)
                .map(tool -> tool == null ? null : McpToolRegistryMapper.toRegistryEntry(tool));
    }

    Uni<McpTool> findAvailableTool(String requestId, String toolId) {
        return toolRepository.findByRequestIdAndToolId(requestId, toolId)
                .map(tool -> tool == null || !tool.isEnabled()
                        ? null
                        : McpToolRegistryMapper.toMcpTool(tool, toolClient));
    }

    private Uni<List<tech.kayys.wayang.tool.entity.McpTool>> filteredRegistryTools(
            String requestId,
            McpToolRegistryFilters filters) {
        return listTenantTools(requestId, filters.namespace()).map(items -> items.stream()
                .filter(filters::matches)
                .toList());
    }

    private Uni<List<tech.kayys.wayang.tool.entity.McpTool>> listTenantTools(String requestId, String namespace) {
        return namespace == null || namespace.isBlank()
                ? toolRepository.findByRequestId(requestId)
                : toolRepository.findByRequestIdAndNamespace(requestId, namespace);
    }
}
