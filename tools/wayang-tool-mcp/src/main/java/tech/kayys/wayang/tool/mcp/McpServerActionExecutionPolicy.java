package tech.kayys.wayang.tool.mcp;

import java.util.function.Predicate;

final class McpServerActionExecutionPolicy {

    static final String REASON_NO_STRUCTURED_OPERATION = "Action has no structured operation.";
    static final String REASON_NOT_SAFE_TO_AUTOMATE = "Action is not marked safe to automate.";
    static final String REASON_UNSUPPORTED_ACTION_CODE =
            "No MCP server action executor supports this action.";

    private McpServerActionExecutionPolicy() {
    }

    static Decision evaluate(
            McpServerActionPreview preview,
            Predicate<String> supportsAction) {
        if (!preview.executable()) {
            return Decision.rejected(REASON_NO_STRUCTURED_OPERATION);
        }
        if (!preview.safeToAutomate()
                || !McpServerActionExecutionMode.AUTOMATABLE.equals(preview.executionMode())) {
            return Decision.rejected(REASON_NOT_SAFE_TO_AUTOMATE);
        }
        if (supportsAction == null || !supportsAction.test(preview.actionCode())) {
            return Decision.rejected(REASON_UNSUPPORTED_ACTION_CODE);
        }
        return Decision.allow();
    }

    record Decision(
            boolean allowed,
            String rejectionReason) {

        private static final Decision ALLOWED = new Decision(true, null);

        static Decision allow() {
            return ALLOWED;
        }

        static Decision rejected(String reason) {
            return new Decision(false, reason);
        }
    }
}
