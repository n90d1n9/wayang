package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

public interface McpServerActionExecutor {

    String actionCode();

    Uni<McpServerActionExecutionResult> execute(
            String requestId,
            McpServerActionPreview preview);

    default boolean supports(String actionCode) {
        return actionCode() != null
                && actionCode().equals(McpServerActionIdentity.normalizeActionCode(actionCode));
    }
}
