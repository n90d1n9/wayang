package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.QueryParam;

public class McpToolRegistryQuery {

    @QueryParam("namespace")
    String namespace;

    @QueryParam("capability")
    String capability;

    @QueryParam("serverName")
    String serverName;

    @QueryParam("enabled")
    Boolean enabled;

    @QueryParam("stale")
    Boolean stale;

    @QueryParam("serverDisabled")
    Boolean serverDisabled;

    @QueryParam("retired")
    Boolean retired;

    @QueryParam("lifecycleState")
    String lifecycleState;

    static McpToolRegistryQuery of(
            String namespace,
            String capability,
            String serverName,
            Boolean enabled,
            Boolean stale,
            Boolean serverDisabled,
            Boolean retired,
            String lifecycleState) {
        McpToolRegistryQuery query = new McpToolRegistryQuery();
        query.namespace = namespace;
        query.capability = capability;
        query.serverName = serverName;
        query.enabled = enabled;
        query.stale = stale;
        query.serverDisabled = serverDisabled;
        query.retired = retired;
        query.lifecycleState = lifecycleState;
        return query;
    }

    McpToolRegistryFilters availableFilters() {
        return McpToolRegistryFilters.available(namespace, capability);
    }

    McpToolRegistryFilters registryFilters() {
        return McpToolRegistryFilters.registry(
                namespace,
                capability,
                serverName,
                enabled,
                stale,
                serverDisabled,
                retired,
                lifecycleState);
    }
}
