package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

public interface McpToolClient {

    Uni<McpToolCallResult> callTool(McpToolInvocation invocation);
}
