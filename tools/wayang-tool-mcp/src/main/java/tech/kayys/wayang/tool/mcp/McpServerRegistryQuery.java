package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.QueryParam;

public class McpServerRegistryQuery {

    @QueryParam("enabled")
    Boolean enabled;

    @QueryParam("transport")
    String transport;

    static McpServerRegistryQuery of(
            Boolean enabled,
            String transport) {
        McpServerRegistryQuery query = new McpServerRegistryQuery();
        query.enabled = enabled;
        query.transport = transport;
        return query;
    }
}
