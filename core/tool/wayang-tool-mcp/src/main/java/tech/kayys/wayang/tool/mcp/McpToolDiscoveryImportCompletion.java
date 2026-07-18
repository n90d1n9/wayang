package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class McpToolDiscoveryImportCompletion {

    private McpToolDiscoveryImportCompletion() {
    }

    static McpToolDiscoveryImportResult discoveryFailure(
            McpToolDiscoveryImportResolution resolution,
            McpToolDiscoveryResult discovery) {
        return McpToolDiscoveryImportResult.failure(
                resolution.request().serverName(),
                resolution.request().effectiveNamespace(),
                discovery.error(),
                discovery.metadata());
    }

    static McpToolDiscoveryImportResult success(
            McpToolDiscoveryImportResolution resolution,
            String namespace,
            McpToolDiscoveryResult discovery,
            McpToolDiscoveryImportChanges changes,
            List<String> staleToolIds) {
        touchRegistryServer(resolution.server());
        return McpToolDiscoveryImportResult.success(
                resolution.request().serverName(),
                namespace,
                discovery.tools().size(),
                changes.toolIds(),
                staleToolIds,
                changes.reactivatedToolIds(),
                McpToolDiscoveryImportPolicy.metadataWithSkippedRetired(
                        discovery.metadata(),
                        changes.skippedRetiredToolIds()));
    }

    static McpToolDiscoveryImportResult failure(
            McpToolDiscoveryImportRequest request,
            String fallbackNamespace,
            Throwable error) {
        return McpToolDiscoveryImportResult.failure(
                request.serverName(),
                fallbackNamespace,
                error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                Map.of());
    }

    private static void touchRegistryServer(McpServerRegistry server) {
        if (server != null) {
            Instant now = Instant.now();
            server.setLastSyncAt(now);
            server.setUpdatedAt(now);
        }
    }
}
