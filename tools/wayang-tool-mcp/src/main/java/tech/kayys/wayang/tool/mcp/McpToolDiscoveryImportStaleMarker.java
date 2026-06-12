package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class McpToolDiscoveryImportStaleMarker {

    private McpToolDiscoveryImportStaleMarker() {
    }

    static Uni<List<String>> markStaleTools(
            ToolRepository toolRepository,
            String requestId,
            McpToolDiscoveryImportRequest request,
            String namespace,
            List<String> activeToolIds) {
        Set<String> active = Set.copyOf(activeToolIds);
        return toolRepository.findByRequestIdAndNamespace(requestId, namespace)
                .map(existingTools -> markStaleTools(request, active, existingTools));
    }

    private static List<String> markStaleTools(
            McpToolDiscoveryImportRequest request,
            Set<String> activeToolIds,
            List<tech.kayys.wayang.tool.entity.McpTool> existingTools) {
        List<String> staleToolIds = new ArrayList<>();
        for (tech.kayys.wayang.tool.entity.McpTool existingTool : existingTools) {
            if (!McpToolDiscoveryImportPolicy.shouldMarkStale(existingTool, request, activeToolIds)) {
                continue;
            }
            McpToolDiscoveryImportPolicy.markStale(existingTool);
            staleToolIds.add(existingTool.getToolId());
        }
        return List.copyOf(staleToolIds);
    }
}
