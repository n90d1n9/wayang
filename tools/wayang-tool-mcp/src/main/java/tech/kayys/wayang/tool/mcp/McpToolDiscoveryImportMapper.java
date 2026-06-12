package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.dto.CapabilityLevel;
import tech.kayys.wayang.tool.dto.HttpMethod;
import tech.kayys.wayang.tool.dto.ToolType;
import tech.kayys.wayang.tool.entity.HttpExecutionConfig;
import tech.kayys.wayang.tool.entity.ToolGuardrails;

import java.time.Instant;

final class McpToolDiscoveryImportMapper {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ACCEPT_JSON_OR_SSE = "application/json, text/event-stream";

    private McpToolDiscoveryImportMapper() {
    }

    static tech.kayys.wayang.tool.entity.McpTool toEntity(
            String requestId,
            McpToolDiscoveryImportRequest request,
            String namespace,
            McpDiscoveredTool discoveredTool) {
        Instant now = Instant.now();
        boolean readOnly = McpToolDiscoveryProtocol.readOnlyHint(discoveredTool);
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId(requestId);
        tool.setToolId(toolId(namespace, discoveredTool.name()));
        tool.setNamespace(namespace);
        tool.setName(discoveredTool.name());
        tool.setDescription(description(discoveredTool));
        tool.setToolType(ToolType.CUSTOM);
        tool.setCapabilityLevel(readOnly ? CapabilityLevel.READ_ONLY : CapabilityLevel.UNKNOWN);
        tool.setInputSchema(discoveredTool.inputSchema());
        tool.setOutputSchema(discoveredTool.outputSchema());
        tool.setExecutionConfig(executionConfig(request));
        tool.setGuardrails(new ToolGuardrails());
        tool.setEnabled(true);
        tool.setReadOnly(readOnly);
        tool.setRequiresApproval(!readOnly);
        tool.setTags(McpToolLifecycle.importTags(request.serverName(), namespace));
        tool.setCapabilities(McpToolLifecycle.importCapabilities(request.serverName(), readOnly));
        tool.setCreatedAt(now);
        tool.setUpdatedAt(now);
        tool.setCreatedBy(request.createdBy());
        tool.setOperationId(discoveredTool.name());
        return tool;
    }

    static tech.kayys.wayang.tool.entity.McpTool updateExisting(
            tech.kayys.wayang.tool.entity.McpTool existing,
            tech.kayys.wayang.tool.entity.McpTool source) {
        existing.setName(source.getName());
        existing.setDescription(source.getDescription());
        existing.setToolType(source.getToolType());
        existing.setCapabilityLevel(source.getCapabilityLevel());
        existing.setInputSchema(source.getInputSchema());
        existing.setOutputSchema(source.getOutputSchema());
        existing.setExecutionConfig(source.getExecutionConfig());
        existing.setGuardrails(source.getGuardrails());
        existing.setEnabled(source.isEnabled());
        existing.setReadOnly(source.isReadOnly());
        existing.setRequiresApproval(source.isRequiresApproval());
        existing.setTags(source.getTags());
        existing.setCapabilities(source.getCapabilities());
        existing.setUpdatedAt(Instant.now());
        existing.setOperationId(source.getOperationId());
        return existing;
    }

    static String endpoint(McpToolDiscoveryImportRequest request) {
        if (request.endpoint() != null && !request.endpoint().isBlank()) {
            return request.endpoint();
        }
        return McpHttpTransportContext.endpoint(request.context()).orElse(null);
    }

    static String toolId(String namespace, String toolName) {
        return namespace + ":" + toolName;
    }

    static String description(McpDiscoveredTool tool) {
        if (tool.description() != null && !tool.description().isBlank()) {
            return tool.description();
        }
        if (tool.title() != null && !tool.title().isBlank()) {
            return tool.title();
        }
        return "MCP tool " + tool.name();
    }

    private static HttpExecutionConfig executionConfig(McpToolDiscoveryImportRequest request) {
        HttpExecutionConfig config = new HttpExecutionConfig();
        config.setMethod(HttpMethod.POST);
        config.setBaseUrl(endpoint(request));
        config.setPath("");
        config.setHeaders(McpHttpTransportContext.headers(request.context()));
        config.setContentType(CONTENT_TYPE_JSON);
        config.setAccept(ACCEPT_JSON_OR_SSE);
        config.setTimeoutMs(McpHttpTransportContext.timeoutMs(request.context()));
        return config;
    }
}
