package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

public record McpToolDiscoveryImportResult(
        boolean success,
        String serverName,
        String namespace,
        int discovered,
        int imported,
        List<String> toolIds,
        int stale,
        List<String> staleToolIds,
        int reactivated,
        List<String> reactivatedToolIds,
        String error,
        Map<String, Object> metadata) {

    public McpToolDiscoveryImportResult {
        toolIds = toolIds == null ? List.of() : List.copyOf(toolIds);
        staleToolIds = staleToolIds == null ? List.of() : List.copyOf(staleToolIds);
        reactivatedToolIds = reactivatedToolIds == null ? List.of() : List.copyOf(reactivatedToolIds);
        metadata = McpMaps.copy(metadata);
    }

    static McpToolDiscoveryImportResult success(
            String serverName,
            String namespace,
            int discovered,
            List<String> toolIds,
            Map<String, Object> metadata) {
        return success(serverName, namespace, discovered, toolIds, List.of(), List.of(), metadata);
    }

    static McpToolDiscoveryImportResult success(
            String serverName,
            String namespace,
            int discovered,
            List<String> toolIds,
            List<String> staleToolIds,
            Map<String, Object> metadata) {
        return success(serverName, namespace, discovered, toolIds, staleToolIds, List.of(), metadata);
    }

    static McpToolDiscoveryImportResult success(
            String serverName,
            String namespace,
            int discovered,
            List<String> toolIds,
            List<String> staleToolIds,
            List<String> reactivatedToolIds,
            Map<String, Object> metadata) {
        return new McpToolDiscoveryImportResult(
                true,
                serverName,
                namespace,
                discovered,
                toolIds.size(),
                toolIds,
                staleToolIds.size(),
                staleToolIds,
                reactivatedToolIds.size(),
                reactivatedToolIds,
                null,
                metadata);
    }

    static McpToolDiscoveryImportResult failure(
            String serverName,
            String namespace,
            String error,
            Map<String, Object> metadata) {
        return new McpToolDiscoveryImportResult(
                false,
                serverName,
                namespace,
                0,
                0,
                List.of(),
                0,
                List.of(),
                0,
                List.of(),
                error,
                metadata);
    }
}
