package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class McpToolDiscoveryClientTestDouble implements McpToolDiscoveryClient {
    private final McpToolDiscoveryResult result;
    private final RuntimeException failure;
    private int calls;
    private McpToolDiscoveryRequest lastRequest;

    McpToolDiscoveryClientTestDouble(McpToolDiscoveryResult result) {
        this(result, null);
    }

    private McpToolDiscoveryClientTestDouble(
            McpToolDiscoveryResult result,
            RuntimeException failure) {
        this.result = result;
        this.failure = failure;
    }

    static McpToolDiscoveryClientTestDouble succeeding(McpToolDiscoveryResult result) {
        return new McpToolDiscoveryClientTestDouble(result);
    }

    static McpToolDiscoveryClientTestDouble failing(RuntimeException failure) {
        return new McpToolDiscoveryClientTestDouble(null, failure);
    }

    int calls() {
        return calls;
    }

    McpToolDiscoveryRequest lastRequest() {
        return lastRequest;
    }

    @Override
    public Uni<McpToolDiscoveryResult> discoverTools(McpToolDiscoveryRequest request) {
        calls++;
        lastRequest = request;
        if (failure != null) {
            return Uni.createFrom().failure(failure);
        }
        return Uni.createFrom().item(result);
    }
}
