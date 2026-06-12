package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;

final class McpServerRegistryInspectionServiceTestDouble extends McpServerRegistryInspectionService {
    private final List<McpServerRegistryEntry> listResult;
    private final McpServerRegistryEntry getResult;
    private String lastRequestId;
    private Boolean lastEnabled;
    private String lastTransport;
    private String lastServerName;

    private McpServerRegistryInspectionServiceTestDouble(
            List<McpServerRegistryEntry> listResult,
            McpServerRegistryEntry getResult) {
        this.listResult = listResult;
        this.getResult = getResult;
    }

    static McpServerRegistryInspectionServiceTestDouble listing(List<McpServerRegistryEntry> result) {
        return new McpServerRegistryInspectionServiceTestDouble(result, null);
    }

    static McpServerRegistryInspectionServiceTestDouble getting(McpServerRegistryEntry result) {
        return new McpServerRegistryInspectionServiceTestDouble(List.of(), result);
    }

    String lastRequestId() {
        return lastRequestId;
    }

    Boolean lastEnabled() {
        return lastEnabled;
    }

    String lastTransport() {
        return lastTransport;
    }

    String lastServerName() {
        return lastServerName;
    }

    @Override
    public Uni<List<McpServerRegistryEntry>> list(
            String requestId,
            Boolean enabled,
            String transport) {
        lastRequestId = requestId;
        lastEnabled = enabled;
        lastTransport = transport;
        return Uni.createFrom().item(listResult);
    }

    @Override
    public Uni<McpServerRegistryEntry> get(String requestId, String serverName) {
        lastRequestId = requestId;
        lastServerName = serverName;
        if (getResult == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(getResult);
    }
}
