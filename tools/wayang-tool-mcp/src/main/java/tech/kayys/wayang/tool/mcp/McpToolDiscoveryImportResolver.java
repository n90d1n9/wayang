package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpToolDiscoveryImportResolver {

    private McpToolDiscoveryImportResolver() {
    }

    static Uni<McpToolDiscoveryImportResolution> resolve(
            String requestId,
            McpToolDiscoveryImportRequest request,
            McpServerRegistryRepository serverRegistryRepository) {
        if (McpToolDiscoveryImportMapper.endpoint(request) != null) {
            return Uni.createFrom().item(new McpToolDiscoveryImportResolution(request, null));
        }
        if (request.serverName() == null || request.serverName().isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP endpoint or serverName is required for discovery import"));
        }
        if (serverRegistryRepository == null) {
            return Uni.createFrom().failure(new IllegalStateException("MCP server registry is not configured"));
        }
        return serverRegistryRepository.findByRequestIdAndName(requestId, request.serverName())
                .flatMap(server -> resolveRegisteredServer(request, server));
    }

    private static Uni<McpToolDiscoveryImportResolution> resolveRegisteredServer(
            McpToolDiscoveryImportRequest request,
            McpServerRegistry server) {
        if (server == null) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP server `" + request.serverName() + "` was not found"));
        }
        if (!server.isEnabled()) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP server `" + request.serverName() + "` is disabled"));
        }
        if (!McpServerTransports.supportsHttpDiscovery(server)) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP server `" + request.serverName() + "` uses unsupported transport `"
                            + server.getTransport() + "` for HTTP discovery import"));
        }
        String endpoint = McpServerEndpoints.url(server);
        if (endpoint == null) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP server `" + request.serverName() + "` does not define a URL"));
        }
        return Uni.createFrom().item(new McpToolDiscoveryImportResolution(
                withRegistryEndpoint(request, endpoint),
                server));
    }

    static McpToolDiscoveryImportRequest withRegistryEndpoint(
            McpToolDiscoveryImportRequest request,
            String endpoint) {
        Map<String, Object> context = new LinkedHashMap<>(request.context());
        context.putIfAbsent(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT, endpoint);
        return new McpToolDiscoveryImportRequest(
                request.serverName(),
                endpoint,
                request.namespace(),
                request.createdBy(),
                context);
    }
}
