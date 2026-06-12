package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.dto.GenerateToolsRequest;
import tech.kayys.wayang.tool.dto.SourceType;
import tech.kayys.wayang.tool.dto.ToolExecutionRequest;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class McpResourceTestFixtures {

    private McpResourceTestFixtures() {
    }

    static ToolRequestContext requestContext(String requestId) {
        ToolRequestContext context = new ToolRequestContext();
        context.setRequestId(requestId);
        return context;
    }

    static ToolGenerationResult toolGenerationResult(String namespace, String... toolIds) {
        List<String> ids = List.of(toolIds);
        return new ToolGenerationResult(
                UUID.randomUUID(),
                namespace,
                ids.size(),
                ids,
                List.of());
    }

    static GenerateToolsRequest generateToolsRequest(String requestId, String userId) {
        return new GenerateToolsRequest(
                requestId,
                "default",
                SourceType.OPENAPI_3_RAW,
                "{}",
                null,
                userId,
                Map.of());
    }

    static ToolExecutionRequest toolExecutionRequest(
            String requestId,
            String userId,
            String toolId,
            Map<String, Object> arguments,
            String runId,
            String agentId) {
        return new ToolExecutionRequest(
                requestId,
                userId,
                toolId,
                arguments,
                runId,
                agentId,
                Map.of());
    }
}
