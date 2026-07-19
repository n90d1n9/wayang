package tech.kayys.wayang.tool.mcp;

public final class McpServerActionPreviewStatus {

    public static final String AUTOMATABLE = "AUTOMATABLE";
    public static final String INVALID = "INVALID";
    public static final String MANUAL = "MANUAL";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";

    private McpServerActionPreviewStatus() {
    }

    static String forAction(McpToolServerHealth.ActionQueueItem action, boolean executable) {
        if (!executable) {
            return MANUAL;
        }
        return action.safeToAutomate() ? AUTOMATABLE : REVIEW_REQUIRED;
    }

    static String reason(String status) {
        return switch (status) {
            case AUTOMATABLE -> "Action has a structured operation and is marked safe to automate.";
            case REVIEW_REQUIRED -> "Action has a structured operation but requires review before execution.";
            case MANUAL -> "Action has no structured operation and must be handled manually.";
            default -> "Action preview is unavailable.";
        };
    }
}
