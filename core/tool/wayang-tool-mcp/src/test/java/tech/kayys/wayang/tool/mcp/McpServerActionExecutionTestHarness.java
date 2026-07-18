package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpResourceTestFixtures.requestContext;

final class McpServerActionExecutionTestHarness {

    private McpServerActionExecutionTestHarness() {
    }

    static McpServerActionRunSyncExecutor runSyncExecutor(
            McpToolServerHealthService healthService,
            McpToolDiscoverySyncService syncService) {
        McpServerActionRunSyncExecutor executor = new McpServerActionRunSyncExecutor();
        executor.serverHealthService = healthService;
        executor.discoverySyncService = syncService;
        return executor;
    }

    static McpServerActionExecutionService runSyncExecutionService(
            McpToolServerHealthService healthService,
            McpToolDiscoverySyncService syncService) {
        return executionService(runSyncExecutor(healthService, syncService));
    }

    static McpServerActionResource actionResource(
            String requestId,
            McpToolServerHealthService healthService,
            McpToolDiscoverySyncService syncService) {
        McpServerActionResource resource = new McpServerActionResource();
        resource.requestContext = requestContext(requestId);
        resource.actionExecutionHistoryService =
                McpServerActionExecutionHistoryResourceTestHarness.newHistoryService();
        resource.serverHealthService = healthService;
        resource.actionExecutionService = runSyncExecutionService(healthService, syncService);
        return resource;
    }

    static McpServerActionExecutionHistoryResource historyResource(McpServerActionResource resource) {
        return McpServerActionExecutionHistoryResourceTestHarness.resourceWithService(
                resource.requestContext,
                resource.actionExecutionHistoryService);
    }

    static McpToolServerHealthService actionHealthService(
            McpToolServerHealth.ActionQueueItem action,
            String expectedRequestId,
            String expectedServerName) {
        return McpServerActionTestFixtures.healthServiceWithOptionalActionFilter(
                action,
                expectedRequestId,
                expectedServerName);
    }

    static McpServerActionExecutionHistoryQueryParamsTestBuilder historyQuery(String actionId) {
        return McpServerActionExecutionHistoryQueryParamsTestBuilder.forAction(actionId);
    }

    static McpToolDiscoverySyncService successfulSyncService(
            String expectedRequestId,
            String expectedServerName,
            McpToolDiscoverySyncResult result) {
        return McpToolDiscoverySyncServiceTestDouble.registered(result)
                .expectingRegisteredServer(expectedRequestId, expectedServerName);
    }

    static McpToolDiscoverySyncService failingSyncService(
            String expectedRequestId,
            String expectedServerName,
            Throwable failure) {
        return McpToolDiscoverySyncServiceTestDouble.failingRegistered(failure)
                .expectingRegisteredServer(expectedRequestId, expectedServerName);
    }

    static McpToolDiscoverySyncService forbiddenSyncService(String message) {
        return McpToolDiscoverySyncServiceTestDouble.forbiddenRegistered(message);
    }

    static McpServerActionExecutionService executionService(McpServerActionExecutor executor) {
        return executionService(List.of(executor));
    }

    static McpServerActionExecutionService executionService(List<McpServerActionExecutor> executors) {
        McpServerActionExecutionService service = new McpServerActionExecutionService();
        service.executorRegistry = executorRegistry(executors);
        return service;
    }

    static McpServerActionExecutorRegistry executorRegistry(McpServerActionExecutor executor) {
        return executorRegistry(List.of(executor));
    }

    static McpServerActionExecutorRegistry executorRegistry(List<McpServerActionExecutor> executors) {
        McpServerActionExecutorRegistry registry = new McpServerActionExecutorRegistry();
        registry.executors = executors;
        return registry;
    }

    static McpServerActionExecutor successfulExecutor(String actionCode) {
        return executor(actionCode, (requestId, preview) -> Uni.createFrom().item(
                McpServerActionExecutionResult.executed(
                        preview,
                        new McpToolDiscoverySyncResult(1, 1, List.of()),
                        McpServerActionQueue.from(McpServerActionTestFixtures.health(preview.action(), List.of())),
                        Instant.EPOCH,
                        Instant.EPOCH)));
    }

    static McpServerActionExecutor executor(
            String actionCode,
            ExecutionHandler handler) {
        return new McpServerActionExecutor() {
            @Override
            public String actionCode() {
                return actionCode;
            }

            @Override
            public Uni<McpServerActionExecutionResult> execute(
                    String requestId,
                    McpServerActionPreview preview) {
                return handler.execute(requestId, preview);
            }
        };
    }

    interface ExecutionHandler {

        Uni<McpServerActionExecutionResult> execute(
                String requestId,
                McpServerActionPreview preview);
    }
}
