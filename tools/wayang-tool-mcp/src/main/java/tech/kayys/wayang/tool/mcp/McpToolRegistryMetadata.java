package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.HttpExecutionConfig;

final class McpToolRegistryMetadata {

    private McpToolRegistryMetadata() {
    }

    static String serverName(tech.kayys.wayang.tool.entity.McpTool tool) {
        return McpToolLifecycle.serverName(tool);
    }

    static String registryEndpoint(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (tool.getExecutionConfig() == null) {
            return null;
        }
        String endpoint = endpoint(tool.getExecutionConfig());
        return endpoint == null || endpoint.isBlank() ? null : endpoint;
    }

    static String endpoint(HttpExecutionConfig config) {
        String baseUrl = config.getBaseUrl() == null ? "" : config.getBaseUrl();
        String path = config.getPath() == null ? "" : config.getPath();
        return baseUrl + path;
    }
}
