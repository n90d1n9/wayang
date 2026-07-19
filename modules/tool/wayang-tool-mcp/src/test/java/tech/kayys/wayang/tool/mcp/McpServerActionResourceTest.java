package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpServerActionResourceTestFixtures.actionQueueQuery;
import static tech.kayys.wayang.tool.mcp.McpServerActionResourceTestFixtures.filteredHealthQuery;
import static tech.kayys.wayang.tool.mcp.McpServerActionResourceTestFixtures.serverHealth;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.actionHealthService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.actionResource;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.forbiddenSyncService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.historyQuery;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.historyResource;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.successfulSyncService;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.health;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.reviewStaleToolsAction;
import static tech.kayys.wayang.tool.mcp.McpResourceTestFixtures.requestContext;

class McpServerActionResourceTest {

    @Test
    void summarizeServerHealthUsesTenantContextAndFilter() {
        McpServerActionResource resource = new McpServerActionResource();
        resource.requestContext = requestContext("tenant-1");
        Instant startedAt = Instant.now();
        McpToolServerHealthServiceTestDouble healthService =
                McpToolServerHealthServiceTestDouble.summarizing(serverHealth("docs", startedAt));
        resource.serverHealthService = healthService;

        RestResponse<McpToolServerHealth> response = resource.summarizeServerHealth(filteredHealthQuery())
                .await().indefinitely();

        McpServerHealthFilters filters = healthService.lastFilters();
        assertEquals("tenant-1", healthService.lastRequestId());
        assertEquals("docs", healthService.lastServerName());
        assertEquals("docs", filters.serverName());
        assertEquals(Boolean.TRUE, filters.enabled());
        assertEquals("HEALTHY", filters.healthStatus());
        assertEquals(Boolean.FALSE, filters.syncDue());
        assertEquals(Boolean.FALSE, filters.hasIssues());
        assertEquals(McpToolServerHealthIssues.ISSUE_STALE_TOOLS, filters.issueCode());
        assertEquals(McpIssueSeverity.WARNING, filters.issueSeverity());
        assertEquals(McpIssueSeverity.WARNING, filters.minIssueSeverity());
        assertEquals(Boolean.FALSE, filters.hasStaleTools());
        assertEquals(Boolean.FALSE, filters.hasServerDisabledTools());
        assertEquals(Boolean.FALSE, filters.hasRetiredTools());
        assertEquals(McpToolLifecycle.LIFECYCLE_ACTIVE, filters.lifecycleState());
        assertEquals(McpServerHealthStatus.DEGRADED, filters.minHealthStatus());
        assertEquals(Boolean.TRUE, filters.attentionRequired());
        assertEquals(McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS, filters.actionCode());
        assertEquals(McpIssueSeverity.WARNING, filters.actionSeverity());
        assertEquals(McpIssueSeverity.WARNING, filters.minActionSeverity());
        assertEquals(Boolean.FALSE, filters.actionSafeToAutomate());
        assertEquals(2, filters.actionQueueLimit());
        assertEquals(1, filters.actionQueueOffset());
        assertEquals(Boolean.TRUE, filters.actionCallable());
        assertEquals("GET", filters.actionMethod());
        assertEquals("/mcp/tools/registry", filters.actionPath());
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED, filters.actionExecutionMode());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().totalServers());
        assertEquals("docs", response.getEntity().servers().getFirst().serverName());
        assertEquals("HEALTHY", response.getEntity().servers().getFirst().healthStatus());
    }

    @Test
    void listServerActionsUsesTenantContextAndReturnsActionQueue() {
        McpServerActionResource resource = new McpServerActionResource();
        resource.requestContext = requestContext("tenant-1");
        McpToolServerHealth.ActionQueueItem action = action("due", McpServerActionCatalog.ACTION_RUN_SYNC);
        McpToolServerHealthServiceTestDouble healthService =
                McpToolServerHealthServiceTestDouble.summarizing(health(action, List.of()));
        resource.serverHealthService = healthService;

        RestResponse<McpServerActionQueue> response = resource.listServerActions(actionQueueQuery("due"))
                .await().indefinitely();

        McpServerHealthFilters filters = healthService.lastFilters();
        assertEquals("tenant-1", healthService.lastRequestId());
        assertEquals("due", filters.serverName());
        assertEquals(Boolean.TRUE, filters.actionCallable());
        assertEquals("POST", filters.actionMethod());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, filters.actionExecutionMode());
        assertEquals(1, filters.actionQueueLimit());
        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().total());
        assertEquals(1, response.getEntity().returned());
        assertEquals(1, response.getEntity().automatableActions());
        assertEquals(1, response.getEntity().callableActions());
        assertEquals(1, response.getEntity().actionMethodCounts().get("POST"));
        assertEquals(1, response.getEntity().actionExecutionModeCounts()
                .get(McpServerActionExecutionMode.AUTOMATABLE));
        assertEquals(List.of(action), response.getEntity().actions());
    }

    @Test
    void previewServerActionUsesActionIdentityAndReturnsReadOnlyPreview() {
        McpServerActionResource resource = new McpServerActionResource();
        resource.requestContext = requestContext("tenant-1");
        McpToolServerHealth.ActionQueueItem action = action("due", McpServerActionCatalog.ACTION_RUN_SYNC);
        McpToolServerHealthServiceTestDouble healthService =
                McpToolServerHealthServiceTestDouble.summarizing(health(action));
        resource.serverHealthService = healthService;

        RestResponse<McpServerActionPreview> response = resource.previewServerAction("due:run-sync")
                .await().indefinitely();

        McpServerHealthFilters filters = healthService.lastFilters();
        assertEquals("tenant-1", healthService.lastRequestId());
        assertEquals("due", filters.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, filters.actionCode());
        assertEquals(200, response.getStatus());
        assertEquals(McpServerActionPreviewStatus.AUTOMATABLE, response.getEntity().status());
        assertEquals(true, response.getEntity().found());
        assertEquals(true, response.getEntity().executable());
        assertEquals(true, response.getEntity().safeToAutomate());
        assertEquals(McpServerActionRiskLevel.LOW, response.getEntity().riskLevel());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, response.getEntity().executionMode());
        assertEquals("due", response.getEntity().serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, response.getEntity().actionCode());
        assertEquals(action.operation(), response.getEntity().operation());
        assertEquals(action, response.getEntity().action());
        assertEquals(List.of("preview warning"), response.getEntity().warnings());
    }

    @Test
    void previewServerActionRejectsMalformedActionId() {
        McpServerActionResource resource = new McpServerActionResource();

        RestResponse<McpServerActionPreview> response = resource.previewServerAction("malformed")
                .await().indefinitely();

        assertEquals(400, response.getStatus());
        assertEquals(McpServerActionPreviewStatus.INVALID, response.getEntity().status());
        assertEquals(false, response.getEntity().found());
        assertEquals(false, response.getEntity().executable());
    }

    @Test
    void executeServerActionRunsAutomatableSyncAction() {
        McpToolServerHealth.ActionQueueItem action = action("due", McpServerActionCatalog.ACTION_RUN_SYNC);
        McpToolServerHealthService healthService = actionHealthService(action, "tenant-1", "due");
        McpToolDiscoverySyncService syncService = successfulSyncService(
                "tenant-1",
                "due",
                new McpToolDiscoverySyncResult(
                        1,
                        2,
                        1,
                        0,
                        List.of("sync warning")));
        McpServerActionResource resource = actionResource("tenant-1", healthService, syncService);

        RestResponse<McpServerActionExecutionResult> response = resource.executeServerAction("due:run-sync")
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, response.getEntity().status());
        assertEquals(true, response.getEntity().executed());
        assertEquals(McpServerActionPreviewStatus.AUTOMATABLE, response.getEntity().preview().status());
        assertEquals(1, response.getEntity().syncResult().scanned());
        assertEquals(2, response.getEntity().syncResult().imported());
        assertNotNull(response.getEntity().startedAt());
        assertNotNull(response.getEntity().finishedAt());
        assertTrue(response.getEntity().durationMs() >= 0);
        assertNotNull(response.getEntity().actionQueueAfter());
        assertEquals(1, response.getEntity().actionQueueAfter().total());
        assertEquals(List.of("preview warning", "sync warning"), response.getEntity().warnings());

        McpServerActionExecutionHistoryResource historyResource = historyResource(resource);
        RestResponse<List<McpServerActionExecutionHistoryEntry>> historyResponse =
                historyResource.listServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .withStatus("executed")
                                        .withExecuted(true)
                                        .withExecutionMode("automatable")
                                        .withRiskLevel("low")
                                        .withWarnings(true)
                                        .withLimit(10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, historyResponse.getStatus());
        assertEquals(1, historyResponse.getEntity().size());
        McpServerActionExecutionHistoryEntry history = historyResponse.getEntity().getFirst();
        assertEquals("due:" + McpServerActionCatalog.ACTION_RUN_SYNC, history.actionId());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, history.status());
        assertEquals(true, history.executed());
        assertEquals("due", history.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, history.actionCode());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, history.executionMode());
        assertEquals(1, history.scanned());
        assertEquals(2, history.imported());
        assertEquals(1, history.actionQueueTotalAfter());
        assertEquals(List.of("preview warning", "sync warning"), history.warnings());

        RestResponse<List<McpServerActionExecutionHistoryEntry>> latestResponse =
                historyResource.listLatestServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .withExecuted(true)
                                        .withExecutionMode("automatable")
                                        .withRiskLevel("low")
                                        .withWarnings(true)
                                        .withPage(0, 10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, latestResponse.getStatus());
        assertEquals(1, latestResponse.getEntity().size());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, latestResponse.getEntity().getFirst().status());

        RestResponse<McpServerActionExecutionHistorySummary> summaryResponse =
                historyResource.summarizeServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .withExecuted(true)
                                        .withExecutionMode("automatable")
                                        .withRiskLevel("low")
                                        .withWarnings(true)
                                        .withStartedAtFrom(history.startedAt().minusSeconds(1).toString())
                                        .withFinishedAtTo(history.finishedAt().plusSeconds(1).toString())
                                        .withLimit(10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, summaryResponse.getStatus());
        assertEquals(1, summaryResponse.getEntity().total());
        assertEquals(1, summaryResponse.getEntity().executed());
        assertEquals(0, summaryResponse.getEntity().rejected());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, summaryResponse.getEntity().latestStatus());
        assertEquals(1, summaryResponse.getEntity().servers().size());
        assertEquals("due", summaryResponse.getEntity().servers().getFirst().serverName());
        assertEquals(1, summaryResponse.getEntity().actions().size());
        assertEquals("due:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                summaryResponse.getEntity().actions().getFirst().actionId());

        RestResponse<McpServerActionExecutionHistoryClearPreview> previewClearResponse =
                historyResource.previewClearServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .build())
                        .await().indefinitely();

        assertEquals(200, previewClearResponse.getStatus());
        assertEquals(1, previewClearResponse.getEntity().matched());
        assertNotNull(previewClearResponse.getEntity().previewedAt());

        RestResponse<McpServerActionExecutionHistoryStats> statsResponse =
                historyResource.getServerActionExecutionHistoryStats()
                        .await().indefinitely();

        assertEquals(200, statsResponse.getStatus());
        assertEquals(1, statsResponse.getEntity().requests());
        assertEquals(1, statsResponse.getEntity().entries());
        assertEquals(500, statsResponse.getEntity().maxEntriesPerRequest());
        assertEquals(604800, statsResponse.getEntity().retentionSeconds());
        assertNotNull(statsResponse.getEntity().oldestEntryAt());
        assertNotNull(statsResponse.getEntity().newestEntryAt());
        assertNotNull(statsResponse.getEntity().inspectedAt());

        RestResponse<McpServerActionExecutionHistoryPruneResult> pruneResponse =
                historyResource.pruneExpiredServerActionExecutions()
                        .await().indefinitely();

        assertEquals(200, pruneResponse.getStatus());
        assertEquals(0, pruneResponse.getEntity().pruned());
        assertNotNull(pruneResponse.getEntity().prunedAt());

        RestResponse<McpServerActionExecutionHistoryClearResult> ignoredClearResponse =
                historyResource.clearServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .withStatus("rejected")
                                        .build())
                        .await().indefinitely();

        assertEquals(200, ignoredClearResponse.getStatus());
        assertEquals(0, ignoredClearResponse.getEntity().cleared());

        RestResponse<McpServerActionExecutionHistoryClearResult> clearResponse =
                historyResource.clearServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .build())
                        .await().indefinitely();

        assertEquals(200, clearResponse.getStatus());
        assertEquals(1, clearResponse.getEntity().cleared());
        assertEquals(true, clearResponse.getEntity().clearedAt() != null);

        RestResponse<List<McpServerActionExecutionHistoryEntry>> clearedHistoryResponse =
                historyResource.listServerActionExecutions(
                                historyQuery("due:run-sync")
                                        .withServerAction("due", "run-sync")
                                        .withLimit(10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, clearedHistoryResponse.getStatus());
        assertEquals(0, clearedHistoryResponse.getEntity().size());
    }

    @Test
    void executeServerActionRejectsReviewRequiredAction() {
        McpToolServerHealth.ActionQueueItem action = reviewStaleToolsAction("docs");
        McpToolServerHealthService healthService = actionHealthService(action, "tenant-1", "docs");
        McpToolDiscoverySyncService syncService =
                forbiddenSyncService("review-required actions must not execute");
        McpServerActionResource resource = actionResource("tenant-1", healthService, syncService);

        RestResponse<McpServerActionExecutionResult> response =
                resource.executeServerAction("docs:review-stale-tools")
                        .await().indefinitely();

        assertEquals(409, response.getStatus());
        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, response.getEntity().status());
        assertEquals(false, response.getEntity().executed());
        assertEquals(McpServerActionExecutionPolicy.REASON_NOT_SAFE_TO_AUTOMATE, response.getEntity().reason());
        assertEquals(McpServerActionPreviewStatus.REVIEW_REQUIRED, response.getEntity().preview().status());
        assertEquals(null, response.getEntity().syncResult());

        McpServerActionExecutionHistoryResource historyResource = historyResource(resource);
        RestResponse<List<McpServerActionExecutionHistoryEntry>> historyResponse =
                historyResource.listServerActionExecutions(
                                historyQuery("docs:review-stale-tools")
                                        .withServerAction("docs", "review-stale-tools")
                                        .withStatus("rejected")
                                        .withExecuted(false)
                                        .withExecutionMode("review-required")
                                        .withRiskLevel("medium")
                                        .withWarnings(true)
                                        .withLimit(10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, historyResponse.getStatus());
        assertEquals(1, historyResponse.getEntity().size());
        McpServerActionExecutionHistoryEntry history = historyResponse.getEntity().getFirst();
        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, history.status());
        assertEquals(false, history.executed());
        assertEquals("docs", history.serverName());
        assertEquals(McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS, history.actionCode());
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED, history.executionMode());
        assertEquals(null, history.scanned());
        assertEquals(null, history.actionQueueTotalAfter());

        RestResponse<McpServerActionExecutionHistorySummary> summaryResponse =
                historyResource.summarizeServerActionExecutions(
                                historyQuery("docs:review-stale-tools")
                                        .withServerAction("docs", "review-stale-tools")
                                        .withStatus("rejected")
                                        .withExecuted(false)
                                        .withExecutionMode("review-required")
                                        .withRiskLevel("medium")
                                        .withWarnings(true)
                                        .withLimit(10)
                                        .build())
                        .await().indefinitely();

        assertEquals(200, summaryResponse.getStatus());
        assertEquals(1, summaryResponse.getEntity().total());
        assertEquals(0, summaryResponse.getEntity().executed());
        assertEquals(1, summaryResponse.getEntity().rejected());
        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, summaryResponse.getEntity().latestStatus());
        assertEquals(McpServerActionExecutionPolicy.REASON_NOT_SAFE_TO_AUTOMATE,
                summaryResponse.getEntity().latestReason());
        assertEquals(1, summaryResponse.getEntity().actions().size());
        assertEquals("docs:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                summaryResponse.getEntity().actions().getFirst().actionId());
    }

}
