package tech.kayys.wayang.agent.mcp;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface McpToolClient {

    Uni<List<McpToolDescriptor>> listTools(McpServerConfig server);

    Uni<McpToolCallResult> callTool(McpToolInvocation invocation);

    default Uni<McpToolCallResult> callTool(McpToolDescriptor tool, Map<String, Object> arguments) {
        return callTool(McpToolInvocation.of(tool, arguments));
    }

    default Uni<Void> disconnect(String serverId) {
        return Uni.createFrom().voidItem();
    }
}
