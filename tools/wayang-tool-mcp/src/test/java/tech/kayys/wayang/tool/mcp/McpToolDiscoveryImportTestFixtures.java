package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

final class McpToolDiscoveryImportTestFixtures {
    private static final String DEFAULT_USER_ID = "user-1";
    private static final String DEFAULT_PROTOCOL_VERSION = "2025-11-25";
    private static final String DEFAULT_ENDPOINT = "http://localhost/mcp";

    private McpToolDiscoveryImportTestFixtures() {
    }

    static McpToolDiscoveryImportRequest registeredImportRequest(String serverName) {
        return importRequest(serverName, null, null, Map.of());
    }

    static McpToolDiscoveryImportRequest httpImportRequest(String serverName) {
        return importRequest(serverName, DEFAULT_ENDPOINT, serverName, Map.of());
    }

    static McpToolDiscoveryImportRequest importRequest(
            String serverName,
            String endpoint,
            String namespace,
            Map<String, Object> context) {
        return new McpToolDiscoveryImportRequest(serverName, endpoint, namespace, DEFAULT_USER_ID, context);
    }

    static McpToolDiscoveryImportRequest importRequest(
            String serverName,
            String endpoint,
            Map<String, Object> context) {
        return importRequest(serverName, endpoint, null, context);
    }

    static McpToolDiscoveryRequest discoveryRequest(String serverName, String endpoint) {
        return new McpToolDiscoveryRequest(serverName, endpoint, Map.of());
    }

    static McpToolDiscoveryImportResult importSuccessResult(
            String serverName,
            String namespace,
            int discovered,
            List<String> toolIds) {
        return McpToolDiscoveryImportResult.success(serverName, namespace, discovered, toolIds, Map.of());
    }

    static McpToolDiscoveryImportResult importFailureResult(
            String serverName,
            String namespace,
            String error) {
        return McpToolDiscoveryImportResult.failure(serverName, namespace, error, Map.of());
    }

    static McpToolDiscoveryResult discoverySuccess(String serverName, McpDiscoveredTool... tools) {
        return discoverySuccess(serverName, 15, Map.of(), tools);
    }

    static McpToolDiscoveryResult discoverySuccess(
            String serverName,
            long durationMs,
            Map<String, Object> metadata,
            McpDiscoveredTool... tools) {
        return McpToolDiscoveryResult.success(
                serverName,
                DEFAULT_PROTOCOL_VERSION,
                List.of(tools),
                durationMs,
                metadata);
    }

    static McpToolDiscoveryResult emptyDiscoverySuccess(String serverName) {
        return McpToolDiscoveryResult.success(
                serverName,
                DEFAULT_PROTOCOL_VERSION,
                List.of(),
                1,
                Map.of());
    }

    static McpToolDiscoveryResult discoveryFailure(String serverName, String error) {
        return McpToolDiscoveryResult.failure(serverName, error, 5, Map.of());
    }

    static McpToolDiscoveryResult discoveryFailure(
            String serverName,
            String error,
            long durationMs,
            Map<String, Object> metadata) {
        return McpToolDiscoveryResult.failure(serverName, error, durationMs, metadata);
    }

    static McpDiscoveredTool discoveredTool(String serverName, String name) {
        return discoveredTool(serverName, name, null, null, Map.of());
    }

    static McpDiscoveredTool discoveredTool(
            String serverName,
            String name,
            String description) {
        return discoveredTool(serverName, name, null, description, Map.of());
    }

    static McpDiscoveredTool discoveredTool(
            String serverName,
            String name,
            String title,
            String description,
            Map<String, Object> metadata) {
        return new McpDiscoveredTool(
                serverName + ":" + name,
                name,
                title,
                description,
                Map.of(McpToolDiscoveryProtocol.FIELD_SCHEMA_TYPE, McpToolDiscoveryProtocol.SCHEMA_OBJECT),
                Map.of(),
                metadata);
    }

    static McpDiscoveredTool readOnlyDiscoveredTool(
            String serverName,
            String name,
            String title,
            String description) {
        return discoveredTool(
                serverName,
                name,
                title,
                description,
                Map.of(
                        McpToolDiscoveryProtocol.FIELD_ANNOTATIONS,
                        Map.of(McpToolDiscoveryProtocol.FIELD_READ_ONLY_HINT, true)));
    }
}
