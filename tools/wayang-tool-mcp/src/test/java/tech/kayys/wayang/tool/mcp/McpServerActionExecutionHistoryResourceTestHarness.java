package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

final class McpServerActionExecutionHistoryResourceTestHarness {
    private static final Clock HISTORY_CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T00:00:00Z"),
            ZoneOffset.UTC);

    private McpServerActionExecutionHistoryResourceTestHarness() {
    }

    static McpServerActionExecutionHistoryService newHistoryService() {
        return historyServiceWithStore(new InMemoryMcpServerActionExecutionHistoryStore(
                Duration.ofDays(7),
                HISTORY_CLOCK));
    }

    static McpServerActionExecutionHistoryService historyServiceWithStore(
            McpServerActionExecutionHistoryStore store) {
        McpServerActionExecutionHistoryService service = new McpServerActionExecutionHistoryService();
        service.historyStore = store;
        return service;
    }

    static McpServerActionExecutionHistoryResource resourceWithService(
            McpServerActionExecutionHistoryService service) {
        return resourceWithService(null, service);
    }

    static McpServerActionExecutionHistoryResource resourceWithService(
            ToolRequestContext requestContext,
            McpServerActionExecutionHistoryService service) {
        McpServerActionExecutionHistoryResource resource =
                new McpServerActionExecutionHistoryResource();
        resource.requestContext = requestContext;
        resource.actionExecutionHistoryService = service;
        return resource;
    }
}
