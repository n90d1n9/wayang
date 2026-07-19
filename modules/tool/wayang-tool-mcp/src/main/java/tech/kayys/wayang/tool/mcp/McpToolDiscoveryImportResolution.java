package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;

record McpToolDiscoveryImportResolution(
        McpToolDiscoveryImportRequest request,
        McpServerRegistry server) {
}
