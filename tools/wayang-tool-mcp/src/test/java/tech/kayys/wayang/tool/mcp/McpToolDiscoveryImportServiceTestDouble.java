package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class McpToolDiscoveryImportServiceTestDouble extends McpToolDiscoveryImportService {
    private final McpToolDiscoveryImportResult result;
    private final RuntimeException failure;
    private int calls;
    private String lastRequestId;
    private McpToolDiscoveryImportRequest lastRequest;

    McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult result) {
        this(result, null);
    }

    private McpToolDiscoveryImportServiceTestDouble(
            McpToolDiscoveryImportResult result,
            RuntimeException failure) {
        this.result = result;
        this.failure = failure;
    }

    static McpToolDiscoveryImportServiceTestDouble succeeding(McpToolDiscoveryImportResult result) {
        return new McpToolDiscoveryImportServiceTestDouble(result);
    }

    static McpToolDiscoveryImportServiceTestDouble failing(RuntimeException failure) {
        return new McpToolDiscoveryImportServiceTestDouble(null, failure);
    }

    int calls() {
        return calls;
    }

    String lastRequestId() {
        return lastRequestId;
    }

    McpToolDiscoveryImportRequest lastRequest() {
        return lastRequest;
    }

    @Override
    public Uni<McpToolDiscoveryImportResult> discoverAndImport(
            String requestId,
            McpToolDiscoveryImportRequest request) {
        calls++;
        lastRequestId = requestId;
        lastRequest = request;
        if (failure != null) {
            return Uni.createFrom().failure(failure);
        }
        return Uni.createFrom().item(result);
    }
}
