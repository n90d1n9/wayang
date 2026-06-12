package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class McpToolDiscoverySyncHistoryQuery {

    @QueryParam("serverName")
    String serverName;

    @QueryParam("status")
    String status;

    @DefaultValue("50")
    @QueryParam("limit")
    int limit;

    static McpToolDiscoverySyncHistoryQuery of(
            String serverName,
            String status,
            int limit) {
        McpToolDiscoverySyncHistoryQuery query = new McpToolDiscoverySyncHistoryQuery();
        query.serverName = serverName;
        query.status = status;
        query.limit = limit;
        return query;
    }
}
