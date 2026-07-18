package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.Map;

final class McpToolClientTestDouble implements McpToolClient {
    private final McpToolCallResult result;
    private final RuntimeException failure;
    private int calls;
    private McpToolInvocation lastInvocation;

    McpToolClientTestDouble(McpToolCallResult result) {
        this(result, null);
    }

    private McpToolClientTestDouble(
            McpToolCallResult result,
            RuntimeException failure) {
        this.result = result;
        this.failure = failure;
    }

    static McpToolClientTestDouble succeeding(McpToolCallResult result) {
        return new McpToolClientTestDouble(result);
    }

    static McpToolClientTestDouble succeedingOk() {
        return succeeding(McpToolCallResult.success(Map.of("ok", true), 1));
    }

    static McpToolClientTestDouble failing(RuntimeException failure) {
        return new McpToolClientTestDouble(null, failure);
    }

    int calls() {
        return calls;
    }

    McpToolInvocation lastInvocation() {
        return lastInvocation;
    }

    @Override
    public Uni<McpToolCallResult> callTool(McpToolInvocation invocation) {
        calls++;
        lastInvocation = invocation;
        if (failure != null) {
            return Uni.createFrom().failure(failure);
        }
        return Uni.createFrom().item(result);
    }
}
