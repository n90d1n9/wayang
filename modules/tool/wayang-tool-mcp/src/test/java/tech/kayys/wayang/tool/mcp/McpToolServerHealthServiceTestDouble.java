package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class McpToolServerHealthServiceTestDouble extends McpToolServerHealthService {
    private final McpToolServerHealth result;
    private String expectedRequestId;
    private String expectedServerName;
    private String expectedActionCode;
    private boolean requireActionCode;
    private String lastRequestId;
    private String lastServerName;
    private McpServerHealthFilters lastFilters;

    private McpToolServerHealthServiceTestDouble(McpToolServerHealth result) {
        this.result = result;
    }

    static McpToolServerHealthServiceTestDouble summarizing(McpToolServerHealth result) {
        return new McpToolServerHealthServiceTestDouble(result);
    }

    McpToolServerHealthServiceTestDouble expectingRequestId(String requestId) {
        expectedRequestId = requestId;
        return this;
    }

    McpToolServerHealthServiceTestDouble expectingServerName(String serverName) {
        expectedServerName = serverName;
        return this;
    }

    McpToolServerHealthServiceTestDouble expectingActionCode(String actionCode, boolean required) {
        expectedActionCode = actionCode;
        requireActionCode = required;
        return this;
    }

    String lastRequestId() {
        return lastRequestId;
    }

    String lastServerName() {
        return lastServerName;
    }

    McpServerHealthFilters lastFilters() {
        return lastFilters;
    }

    @Override
    public Uni<McpToolServerHealth> summarize(String requestId, String serverName) {
        lastServerName = serverName;
        return summarize(requestId, McpServerHealthFilters.byServerName(serverName));
    }

    @Override
    public Uni<McpToolServerHealth> summarize(String requestId, McpServerHealthFilters filters) {
        lastRequestId = requestId;
        lastFilters = filters;
        if (filters != null) {
            lastServerName = filters.serverName();
        }
        assertExpected(requestId, filters);
        if (result == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(result);
    }

    private void assertExpected(String requestId, McpServerHealthFilters filters) {
        if (expectedRequestId != null) {
            assertEquals(expectedRequestId, requestId);
        }
        if (filters == null) {
            return;
        }
        if (expectedServerName != null) {
            assertEquals(expectedServerName, filters.serverName());
        }
        if (expectedActionCode != null && (requireActionCode || filters.actionCode() != null)) {
            assertEquals(expectedActionCode, filters.actionCode());
        }
    }
}
