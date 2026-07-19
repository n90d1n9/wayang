package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class McpHistoryPruneSchedulerTestHarness {

    private McpHistoryPruneSchedulerTestHarness() {
    }

    static McpToolCallHistoryPruneScheduler toolCallHistoryScheduler(
            McpToolCallHistoryStore store,
            boolean enabled) {
        McpToolCallHistoryPruneScheduler scheduler = new McpToolCallHistoryPruneScheduler();
        scheduler.toolCallHistoryService = new McpToolCallHistoryService(store);
        scheduler.enabled = enabled;
        return scheduler;
    }

    static McpServerActionExecutionHistoryPruneScheduler actionExecutionHistoryScheduler(
            McpServerActionExecutionHistoryService service,
            boolean enabled) {
        McpServerActionExecutionHistoryPruneScheduler scheduler =
                new McpServerActionExecutionHistoryPruneScheduler();
        scheduler.actionExecutionHistoryService = service;
        scheduler.enabled = enabled;
        return scheduler;
    }

    static ToolCallHistoryCountingStore toolCallHistoryCountingStore() {
        return new ToolCallHistoryCountingStore();
    }

    static ActionExecutionHistoryCountingStore actionExecutionHistoryCountingStore() {
        return new ActionExecutionHistoryCountingStore();
    }

    static final class ToolCallHistoryCountingStore extends InMemoryMcpToolCallHistoryStore {
        private int pruneExpiredCalls;

        @Override
        public Uni<Integer> pruneExpired() {
            pruneExpiredCalls++;
            return Uni.createFrom().item(99);
        }

        int pruneExpiredCalls() {
            return pruneExpiredCalls;
        }
    }

    static final class ActionExecutionHistoryCountingStore
            extends InMemoryMcpServerActionExecutionHistoryStore {
        private int pruneExpiredCalls;

        @Override
        public Uni<Integer> pruneExpired() {
            pruneExpiredCalls++;
            return Uni.createFrom().item(99);
        }

        int pruneExpiredCalls() {
            return pruneExpiredCalls;
        }
    }
}
