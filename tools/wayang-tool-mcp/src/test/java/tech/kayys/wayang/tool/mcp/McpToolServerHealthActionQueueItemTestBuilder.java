package tech.kayys.wayang.tool.mcp;

final class McpToolServerHealthActionQueueItemTestBuilder {
    private final String serverName;
    private final String code;
    private String healthStatus;
    private String transport = "http";
    private String endpoint;
    private String severity = McpIssueSeverity.INFO;
    private int priority;
    private boolean safeToAutomate;
    private String message;
    private String actionHint;
    private McpToolServerHealth.ActionOperation operation;

    private McpToolServerHealthActionQueueItemTestBuilder(String serverName, String code) {
        this.serverName = serverName;
        this.code = code;
        this.endpoint = "http://" + serverName + ".local/mcp";
    }

    static McpToolServerHealthActionQueueItemTestBuilder actionQueueItem(
            String serverName,
            String code) {
        return new McpToolServerHealthActionQueueItemTestBuilder(serverName, code);
    }

    McpToolServerHealthActionQueueItemTestBuilder withHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withEndpoint(String transport, String endpoint) {
        this.transport = transport;
        this.endpoint = endpoint;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withSeverity(String severity) {
        this.severity = severity;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withSafeToAutomate(boolean safeToAutomate) {
        this.safeToAutomate = safeToAutomate;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withActionHint(String actionHint) {
        this.actionHint = actionHint;
        return this;
    }

    McpToolServerHealthActionQueueItemTestBuilder withOperation(
            McpToolServerHealth.ActionOperation operation) {
        this.operation = operation;
        return this;
    }

    McpToolServerHealth.ActionQueueItem build() {
        return new McpToolServerHealth.ActionQueueItem(
                serverName + ":" + code,
                serverName,
                healthStatus,
                transport,
                endpoint,
                code,
                severity,
                priority,
                safeToAutomate,
                message,
                actionHint,
                operation);
    }
}
