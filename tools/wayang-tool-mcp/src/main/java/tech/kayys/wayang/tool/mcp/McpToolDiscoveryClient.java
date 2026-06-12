package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

public interface McpToolDiscoveryClient {

    Uni<McpToolDiscoveryResult> discoverTools(McpToolDiscoveryRequest request);
}
