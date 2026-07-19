package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpServerActionPreview(
        String actionId,
        String status,
        boolean found,
        boolean executable,
        boolean safeToAutomate,
        String executionMode,
        String riskLevel,
        String reason,
        String serverName,
        String actionCode,
        String severity,
        int priority,
        McpToolServerHealth.ActionOperation operation,
        McpToolServerHealth.ActionQueueItem action,
        List<String> warnings) {

    
    public McpServerActionPreview {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    static McpServerActionPreview invalid(String actionId) {
        return new McpServerActionPreview(
                actionId,
                McpServerActionPreviewStatus.INVALID,
                false,
                false,
                false,
                null,
                McpServerActionRiskLevel.UNKNOWN,
                "Action id must use '<serverName>:<actionCode>'.",
                null,
                null,
                null,
                0,
                null,
                null,
                List.of());
    }

    static McpServerActionPreview notFound(McpServerActionIdentity identity, List<String> warnings) {
        return new McpServerActionPreview(
                identity.actionId(),
                McpServerActionPreviewStatus.NOT_FOUND,
                false,
                false,
                false,
                null,
                McpServerActionRiskLevel.UNKNOWN,
                "Action is not present in the current MCP server action queue.",
                identity.serverName(),
                identity.actionCode(),
                null,
                0,
                null,
                null,
                warnings);
    }

    static McpServerActionPreview from(
            McpServerActionIdentity identity,
            McpToolServerHealth.ActionQueueItem action,
            List<String> warnings) {
        if (action == null) {
            return notFound(identity, warnings);
        }
        boolean executable = action.operation() != null;
        String status = McpServerActionPreviewStatus.forAction(action, executable);
        return new McpServerActionPreview(
                action.id(),
                status,
                true,
                executable,
                action.safeToAutomate(),
                action.executionMode(),
                McpServerActionRiskLevel.from(action),
                McpServerActionPreviewStatus.reason(status),
                action.serverName(),
                action.code(),
                action.severity(),
                action.priority(),
                action.operation(),
                action,
                warnings);
    }

    static McpServerActionIdentity identity(String actionId) {
        return McpServerActionIdentity.parse(actionId);
    }

}
