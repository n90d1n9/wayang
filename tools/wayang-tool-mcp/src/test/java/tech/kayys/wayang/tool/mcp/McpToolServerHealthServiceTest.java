package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpServerHealthFiltersTestBuilder.filters;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthActionQueueItemTestBuilder.actionQueueItem;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthServiceTestHarness.disabledRegistryHealth;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthServiceTestHarness.healthWith;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthServiceTestHarness.healthWithServers;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.healthServer;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.issueDetail;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.recommendedAction;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthServiceTest {

    @Test
    void summarizesServersToolsAndLatestSyncHistory() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                healthServer("tenant-1", "docs", "http", "http://docs.local/mcp", true),
                healthServer("tenant-1", "crm", "http", "http://crm.local/mcp", true),
                healthServer("tenant-1", "files", "stdio", "node files.js", false)));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(
                tool("tenant-1", "docs.search", true, Set.of("search", "mcp:docs"), Set.of()),
                tool("tenant-1", "docs.old", false, Set.of("search", "stale", "mcp:docs"), Set.of("stale")),
                tool("tenant-1", "docs.paused", false, Set.of("search", "mcp:docs"),
                        Set.of(McpToolLifecycle.SERVER_DISABLED_TAG)),
                tool("tenant-1", "docs.retired", false, Set.of("search", "stale", "mcp:docs"),
                        Set.of(McpToolLifecycle.SERVER_RETIRED_TAG)),
                tool("tenant-1", "crm.lookup", true, Set.of("search", "mcp:crm"), Set.of())));
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(
                        history("docs", "SUCCESS", "docs synced", 2, now),
                        history("crm", "ERROR", "crm blocked", 0, now.plusSeconds(1))),
                List.of(history("docs", "SUCCESS", "docs synced", 2, now)),
                List.of(history("crm", "ERROR", "crm blocked", 0, now.plusSeconds(1))));

        McpToolServerHealthServiceTestHarness health = healthWith(serverRepository, toolRepository, syncService);

        McpToolServerHealth result = health.summarize();
        McpToolServerHealth warningOrWorse = health.summarize(
                filters()
                        .withMinIssueSeverity("warning")
                        .build());
        McpToolServerHealth degradedOrWorse = health.summarize(
                filters()
                        .withMinHealthStatus("degraded")
                        .build());
        McpToolServerHealth attentionHealth = health.summarize(
                filters()
                        .withAttentionRequired(true)
                        .build());
        McpToolServerHealth staleActionHealth = health.summarize(
                McpServerHealthFilters.byActionCode("review-stale-tools"));
        McpToolServerHealth limitedActionHealth = health.summarize(
                McpServerHealthFilters.byActionQueueLimit(2));
        McpToolServerHealth pagedActionHealth = health.summarize(
                McpServerHealthFilters.byActionQueueWindow(1, 2));
        McpToolServerHealth tailActionHealth = health.summarize(
                McpServerHealthFilters.byActionQueueWindow(3, 2));
        McpToolServerHealth callableActionHealth = health.summarize(
                McpServerHealthFilters.byActionCallable(true));
        McpToolServerHealth postActionHealth = health.summarize(
                McpServerHealthFilters.byActionMethod("post"));
        McpToolServerHealth reviewRequiredActionHealth = health.summarize(
                McpServerHealthFilters.byActionExecutionMode("review-required"));

        assertEquals(3, result.totalServers());
        assertEquals(2, result.enabledServers());
        assertEquals(1, result.disabledServers());
        assertEquals(0, result.healthyServers());
        assertEquals(1, result.degradedServers());
        assertEquals(1, result.unhealthyServers());
        assertEquals(0, result.unsyncedServers());
        assertEquals(2, result.attentionRequiredServers());
        assertEquals(McpServerHealthStatus.UNHEALTHY, result.highestHealthStatus());
        assertEquals(0, result.healthStatusCounts().get(McpServerHealthStatus.HEALTHY));
        assertEquals(1, result.healthStatusCounts().get(McpServerHealthStatus.DEGRADED));
        assertEquals(1, result.healthStatusCounts().get(McpServerHealthStatus.UNHEALTHY));
        assertEquals(0, result.healthStatusCounts().get(McpServerHealthStatus.UNSYNCED));
        assertEquals(1, result.healthStatusCounts().get(McpServerHealthStatus.DISABLED));
        assertEquals(5, result.totalTools());
        assertEquals(2, result.enabledTools());
        assertEquals(3, result.disabledTools());
        assertEquals(2, result.staleTools());
        assertEquals(2, result.activeTools());
        assertEquals(1, result.serverDisabledTools());
        assertEquals(1, result.retiredTools());
        assertEquals(2, result.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(0, result.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_DISABLED));
        assertEquals(1, result.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED));
        assertEquals(1, result.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        assertEquals(1, result.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_RETIRED));
        assertEquals(1, result.issueCounts().get(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED));
        assertEquals(1, result.issueCounts().get(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED_TOOLS));
        assertEquals(1, result.issueCounts().get(McpToolServerHealthIssues.ISSUE_STALE_TOOLS));
        assertEquals(1, result.issueCounts().get(McpToolServerHealthIssues.ISSUE_SYNC_ERROR));
        assertEquals(1, result.issueSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(1, result.issueSeverityCounts().get(McpIssueSeverity.INFO));
        assertEquals(2, result.issueSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.CRITICAL, result.highestIssueSeverity());
        assertEquals(1, result.actionCounts().get(McpServerActionCatalog.ACTION_CHECK_ENDPOINT));
        assertEquals(1, result.actionCounts().get(McpServerActionCatalog.ACTION_ENABLE_SERVER));
        assertEquals(1, result.actionCounts().get(McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS));
        assertEquals(1, result.actionCounts()
                .get(McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS));
        assertEquals(4, result.recommendedActions());
        assertEquals(0, result.automatableActions());
        assertEquals(4, result.manualActions());
        assertEquals(3, result.callableActions());
        assertEquals(1, result.nonCallableActions());
        assertEquals(2, result.actionMethodCounts().get("GET"));
        assertEquals(1, result.actionMethodCounts().get("POST"));
        assertEquals(1, result.actionExecutionModeCounts().get(McpServerActionExecutionMode.MANUAL));
        assertEquals(3, result.actionExecutionModeCounts()
                .get(McpServerActionExecutionMode.REVIEW_REQUIRED));
        assertEquals(1, result.actionSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(1, result.actionSeverityCounts().get(McpIssueSeverity.INFO));
        assertEquals(2, result.actionSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.CRITICAL, result.highestActionSeverity());
        assertEquals(4, result.actionQueueTotal());
        assertEquals(0, result.actionQueueOffset());
        assertEquals(null, result.actionQueueLimit());
        assertEquals(4, result.actionQueueReturned());
        assertEquals(false, result.actionQueueTruncated());
        assertEquals(List.of(
                actionQueueItem("crm", McpServerActionCatalog.ACTION_CHECK_ENDPOINT)
                        .withHealthStatus(McpServerHealthStatus.UNHEALTHY)
                        .withSeverity(McpIssueSeverity.CRITICAL)
                        .withPriority(300)
                        .withMessage("Check MCP endpoint, credentials, and transport logs.")
                        .withActionHint("Inspect http://crm.local/mcp")
                        .build(),
                actionQueueItem("docs", McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS)
                        .withHealthStatus(McpServerHealthStatus.DEGRADED)
                        .withSeverity(McpIssueSeverity.WARNING)
                        .withPriority(200)
                        .withMessage("Review 2 stale MCP tool(s).")
                        .withActionHint("GET /mcp/tools/registry?serverName=docs&stale=true")
                        .withOperation(operation(
                                "GET",
                                "/mcp/tools/registry",
                                Map.of("serverName", "docs", "stale", "true")))
                        .build(),
                actionQueueItem("docs", McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS)
                        .withHealthStatus(McpServerHealthStatus.DEGRADED)
                        .withSeverity(McpIssueSeverity.WARNING)
                        .withPriority(200)
                        .withMessage("Review 1 server-disabled MCP tool(s).")
                        .withActionHint("GET /mcp/tools/registry?serverName=docs&serverDisabled=true")
                        .withOperation(operation(
                                "GET",
                                "/mcp/tools/registry",
                                Map.of("serverName", "docs", "serverDisabled", "true")))
                        .build(),
                actionQueueItem("files", McpServerActionCatalog.ACTION_ENABLE_SERVER)
                        .withHealthStatus(McpServerHealthStatus.DISABLED)
                        .withEndpoint("stdio", "node files.js")
                        .withPriority(100)
                        .withMessage("Enable this MCP server before using its imported tools.")
                        .withActionHint("POST /mcp/servers/files/enable")
                        .withOperation(operation("POST", "/mcp/servers/files/enable"))
                        .build()),
                result.actionQueue());
        assertEquals(4, limitedActionHealth.recommendedActions());
        assertEquals(4, limitedActionHealth.actionQueueTotal());
        assertEquals(0, limitedActionHealth.actionQueueOffset());
        assertEquals(2, limitedActionHealth.actionQueueLimit());
        assertEquals(2, limitedActionHealth.actionQueueReturned());
        assertEquals(true, limitedActionHealth.actionQueueTruncated());
        assertEquals(2, limitedActionHealth.actionQueue().size());
        assertEquals(List.of(
                "crm:" + McpServerActionCatalog.ACTION_CHECK_ENDPOINT,
                "docs:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS),
                limitedActionHealth.actionQueue().stream()
                        .map(McpToolServerHealth.ActionQueueItem::id)
                        .toList());
        assertEquals(4, pagedActionHealth.actionQueueTotal());
        assertEquals(1, pagedActionHealth.actionQueueOffset());
        assertEquals(2, pagedActionHealth.actionQueueLimit());
        assertEquals(2, pagedActionHealth.actionQueueReturned());
        assertEquals(true, pagedActionHealth.actionQueueTruncated());
        assertEquals(List.of(
                "docs:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                "docs:" + McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS),
                pagedActionHealth.actionQueue().stream()
                        .map(McpToolServerHealth.ActionQueueItem::id)
                        .toList());
        assertEquals(4, tailActionHealth.actionQueueTotal());
        assertEquals(3, tailActionHealth.actionQueueOffset());
        assertEquals(2, tailActionHealth.actionQueueLimit());
        assertEquals(1, tailActionHealth.actionQueueReturned());
        assertEquals(false, tailActionHealth.actionQueueTruncated());
        assertEquals(List.of("files:" + McpServerActionCatalog.ACTION_ENABLE_SERVER),
                tailActionHealth.actionQueue().stream()
                        .map(McpToolServerHealth.ActionQueueItem::id)
                        .toList());
        assertEquals(2, callableActionHealth.totalServers());
        assertEquals(3, callableActionHealth.callableActions());
        assertEquals(0, callableActionHealth.nonCallableActions());
        assertEquals(List.of("docs", "files"), callableActionHealth.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());
        assertEquals(1, postActionHealth.totalServers());
        assertEquals(1, postActionHealth.callableActions());
        assertEquals(1, postActionHealth.actionMethodCounts().get("POST"));
        assertEquals("files", postActionHealth.servers().getFirst().serverName());
        assertEquals(2, reviewRequiredActionHealth.totalServers());
        assertEquals(3, reviewRequiredActionHealth.actionExecutionModeCounts()
                .get(McpServerActionExecutionMode.REVIEW_REQUIRED));
        assertEquals(List.of("docs", "files"), reviewRequiredActionHealth.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());
        assertEquals(2, warningOrWorse.totalServers());
        assertEquals(2, warningOrWorse.attentionRequiredServers());
        assertEquals(McpServerHealthStatus.UNHEALTHY, warningOrWorse.highestHealthStatus());
        assertEquals(1, warningOrWorse.healthStatusCounts().get(McpServerHealthStatus.DEGRADED));
        assertEquals(1, warningOrWorse.healthStatusCounts().get(McpServerHealthStatus.UNHEALTHY));
        assertEquals(McpIssueSeverity.CRITICAL, warningOrWorse.highestIssueSeverity());
        assertEquals(List.of("crm", "docs"), warningOrWorse.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());
        assertEquals(2, degradedOrWorse.totalServers());
        assertEquals(2, degradedOrWorse.attentionRequiredServers());
        assertEquals(McpServerHealthStatus.UNHEALTHY, degradedOrWorse.highestHealthStatus());
        assertEquals(1, degradedOrWorse.healthStatusCounts().get(McpServerHealthStatus.DEGRADED));
        assertEquals(1, degradedOrWorse.healthStatusCounts().get(McpServerHealthStatus.UNHEALTHY));
        assertEquals(List.of("crm", "docs"), degradedOrWorse.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());
        assertEquals(2, attentionHealth.totalServers());
        assertEquals(List.of("crm", "docs"), attentionHealth.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());
        assertEquals(1, staleActionHealth.totalServers());
        assertEquals("docs", staleActionHealth.servers().getFirst().serverName());

        McpToolServerHealth.ServerHealth docs = result.servers().stream()
                .filter(server -> "docs".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("DEGRADED", docs.healthStatus());
        assertEquals(true, docs.attentionRequired());
        assertEquals(List.of(
                recommendedAction(
                        McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                        McpIssueSeverity.WARNING,
                        false,
                        "Review 2 stale MCP tool(s).",
                        "GET /mcp/tools/registry?serverName=docs&stale=true",
                        operation(
                                "GET",
                                "/mcp/tools/registry",
                                Map.of("serverName", "docs", "stale", "true"))),
                recommendedAction(
                        McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS,
                        McpIssueSeverity.WARNING,
                        false,
                        "Review 1 server-disabled MCP tool(s).",
                        "GET /mcp/tools/registry?serverName=docs&serverDisabled=true",
                        operation(
                                "GET",
                                "/mcp/tools/registry",
                                Map.of("serverName", "docs", "serverDisabled", "true")))),
                docs.recommendedActions());
        assertEquals(docs.lastSyncAt().plus(Duration.ofMinutes(5)), docs.nextSyncAt());
        assertEquals(false, docs.syncDue());
        assertEquals(null, docs.syncScheduleError());
        assertEquals(0, docs.consecutiveFailures());
        assertEquals(List.of(
                "2 stale MCP tool(s) need review.",
                "1 server-disabled MCP tool(s) need review."),
                docs.issues());
        assertEquals(List.of(
                McpToolServerHealthIssues.ISSUE_STALE_TOOLS,
                McpToolServerHealthIssues.ISSUE_SERVER_DISABLED_TOOLS),
                docs.issueCodes());
        assertEquals(List.of(
                issueDetail(
                        McpToolServerHealthIssues.ISSUE_STALE_TOOLS,
                        McpIssueSeverity.WARNING,
                        "2 stale MCP tool(s) need review."),
                issueDetail(
                        McpToolServerHealthIssues.ISSUE_SERVER_DISABLED_TOOLS,
                        McpIssueSeverity.WARNING,
                        "1 server-disabled MCP tool(s) need review.")),
                docs.issueDetails());
        assertEquals(2, docs.issueSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.WARNING, docs.highestIssueSeverity());
        assertEquals("SUCCESS", docs.latestSyncStatus());
        assertEquals(now.plusMillis(10), docs.lastSuccessAt());
        assertEquals(4, docs.totalTools());
        assertEquals(2, docs.staleTools());
        assertEquals(1, docs.activeTools());
        assertEquals(1, docs.serverDisabledTools());
        assertEquals(1, docs.retiredTools());
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(0, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_DISABLED));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_RETIRED));

        McpToolServerHealth.ServerHealth crm = result.servers().stream()
                .filter(server -> "crm".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("UNHEALTHY", crm.healthStatus());
        assertEquals(true, crm.attentionRequired());
        assertEquals(List.of(recommendedAction(
                McpServerActionCatalog.ACTION_CHECK_ENDPOINT,
                McpIssueSeverity.CRITICAL,
                false,
                "Check MCP endpoint, credentials, and transport logs.",
                "Inspect http://crm.local/mcp")), crm.recommendedActions());
        assertEquals(1, crm.consecutiveFailures());
        assertEquals(List.of("Latest sync failed: crm blocked"), crm.issues());
        assertEquals(List.of(McpToolServerHealthIssues.ISSUE_SYNC_ERROR), crm.issueCodes());
        assertEquals(List.of(issueDetail(
                McpToolServerHealthIssues.ISSUE_SYNC_ERROR,
                McpIssueSeverity.CRITICAL,
                "Latest sync failed: crm blocked")), crm.issueDetails());
        assertEquals(1, crm.issueSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(McpIssueSeverity.CRITICAL, crm.highestIssueSeverity());
        assertEquals("ERROR", crm.latestSyncStatus());
        assertEquals(now.plusSeconds(1).plusMillis(10), crm.lastErrorAt());

        McpToolServerHealth.ServerHealth files = result.servers().stream()
                .filter(server -> "files".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("DISABLED", files.healthStatus());
        assertEquals(false, files.attentionRequired());
        assertEquals(List.of(recommendedAction(
                McpServerActionCatalog.ACTION_ENABLE_SERVER,
                McpIssueSeverity.INFO,
                false,
                "Enable this MCP server before using its imported tools.",
                "POST /mcp/servers/files/enable",
                operation("POST", "/mcp/servers/files/enable"))), files.recommendedActions());
    }

    @Test
    void summarizeExposesDueAndInvalidScheduleState() {
        Instant lastSyncAt = Instant.now().minus(Duration.ofMinutes(10));
        McpServerRegistry due = healthServer("tenant-1", "due", "http", "http://due.local/mcp", true);
        due.setSyncSchedule("PT5M");
        due.setLastSyncAt(lastSyncAt);
        McpServerRegistry invalid = healthServer("tenant-1", "invalid", "http", "http://invalid.local/mcp", true);
        invalid.setSyncSchedule("not-an-interval");
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(due, invalid));
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(
                        history("due", "SUCCESS", "due synced", 1, lastSyncAt),
                        history("invalid", "SUCCESS", "invalid synced", 1, lastSyncAt)),
                List.of(
                        history("due", "SUCCESS", "due synced", 1, lastSyncAt),
                        history("invalid", "SUCCESS", "invalid synced", 1, lastSyncAt)),
                List.of());

        McpToolServerHealthServiceTestHarness health = healthWithServers(serverRepository, syncService);

        McpToolServerHealth result = health.summarize();
        McpToolServerHealth automatableActionHealth = health.summarize(
                McpServerHealthFilters.byActionSafeToAutomate(true));
        McpToolServerHealth warningActionHealth = health.summarize(
                McpServerHealthFilters.byMinActionSeverity("warning"));
        McpToolServerHealth postActionHealth = health.summarize(
                McpServerHealthFilters.byActionMethod("post"));
        McpToolServerHealth automatableExecutionHealth = health.summarize(
                McpServerHealthFilters.byActionExecutionMode("automatable"));

        assertEquals(2, result.recommendedActions());
        assertEquals(1, result.automatableActions());
        assertEquals(1, result.manualActions());
        assertEquals(1, result.callableActions());
        assertEquals(1, result.nonCallableActions());
        assertEquals(1, result.actionMethodCounts().get("POST"));
        assertEquals(1, result.actionExecutionModeCounts().get(McpServerActionExecutionMode.AUTOMATABLE));
        assertEquals(1, result.actionExecutionModeCounts().get(McpServerActionExecutionMode.MANUAL));
        assertEquals(1, result.actionSeverityCounts().get(McpIssueSeverity.INFO));
        assertEquals(1, result.actionSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.WARNING, result.highestActionSeverity());
        assertEquals(2, result.actionQueueTotal());
        assertEquals(0, result.actionQueueOffset());
        assertEquals(null, result.actionQueueLimit());
        assertEquals(2, result.actionQueueReturned());
        assertEquals(false, result.actionQueueTruncated());
        assertEquals(List.of(
                actionQueueItem("invalid", McpServerActionCatalog.ACTION_FIX_SYNC_SCHEDULE)
                        .withHealthStatus(McpServerHealthStatus.DEGRADED)
                        .withSeverity(McpIssueSeverity.WARNING)
                        .withPriority(200)
                        .withMessage("Fix the MCP sync schedule.")
                        .withActionHint("Update registry syncSchedule for invalid")
                        .build(),
                actionQueueItem("due", McpServerActionCatalog.ACTION_RUN_SYNC)
                        .withHealthStatus(McpServerHealthStatus.HEALTHY)
                        .withPriority(110)
                        .withSafeToAutomate(true)
                        .withMessage("Run scheduled MCP discovery sync for this server.")
                        .withActionHint("POST /mcp/tools/discover/sync/due")
                        .withOperation(operation("POST", "/mcp/tools/discover/sync/due"))
                        .build()),
                result.actionQueue());
        assertEquals(1, automatableActionHealth.totalServers());
        assertEquals("due", automatableActionHealth.servers().getFirst().serverName());
        assertEquals(1, automatableActionHealth.automatableActions());
        assertEquals(1, warningActionHealth.totalServers());
        assertEquals("invalid", warningActionHealth.servers().getFirst().serverName());
        assertEquals(1, warningActionHealth.nonCallableActions());
        assertEquals(1, postActionHealth.totalServers());
        assertEquals("due", postActionHealth.servers().getFirst().serverName());
        assertEquals(1, postActionHealth.callableActions());
        assertEquals(1, automatableExecutionHealth.totalServers());
        assertEquals("due", automatableExecutionHealth.servers().getFirst().serverName());
        assertEquals(1, automatableExecutionHealth.actionExecutionModeCounts()
                .get(McpServerActionExecutionMode.AUTOMATABLE));

        McpToolServerHealth.ServerHealth dueHealth = result.servers().stream()
                .filter(server -> "due".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals(lastSyncAt.plus(Duration.ofMinutes(5)), dueHealth.nextSyncAt());
        assertEquals(true, dueHealth.syncDue());
        assertEquals(null, dueHealth.syncScheduleError());
        assertEquals(List.of(recommendedAction(
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpIssueSeverity.INFO,
                true,
                "Run scheduled MCP discovery sync for this server.",
                "POST /mcp/tools/discover/sync/due",
                operation("POST", "/mcp/tools/discover/sync/due"))), dueHealth.recommendedActions());

        McpToolServerHealth.ServerHealth invalidHealth = result.servers().stream()
                .filter(server -> "invalid".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("DEGRADED", invalidHealth.healthStatus());
        assertEquals(null, invalidHealth.nextSyncAt());
        assertEquals(false, invalidHealth.syncDue());
        assertEquals("Invalid interval format: not-an-interval. Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m).",
                invalidHealth.syncScheduleError());
        assertEquals(List.of("Sync schedule is invalid: Invalid interval format: not-an-interval. Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m)."),
                invalidHealth.issues());
        assertEquals(List.of(McpToolServerHealthIssues.ISSUE_INVALID_SYNC_SCHEDULE), invalidHealth.issueCodes());
        assertEquals(List.of(issueDetail(
                McpToolServerHealthIssues.ISSUE_INVALID_SYNC_SCHEDULE,
                McpIssueSeverity.WARNING,
                "Sync schedule is invalid: Invalid interval format: not-an-interval. Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m).")),
                invalidHealth.issueDetails());
        assertEquals(List.of(recommendedAction(
                McpServerActionCatalog.ACTION_FIX_SYNC_SCHEDULE,
                McpIssueSeverity.WARNING,
                false,
                "Fix the MCP sync schedule.",
                "Update registry syncSchedule for invalid")), invalidHealth.recommendedActions());
    }

    @Test
    void summarizeCountsConsecutiveFailuresFromRecentHistory() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                healthServer("tenant-1", "crm", "http", "http://crm.local/mcp", true)));
        McpToolDiscoverySyncHistoryEntry latestError = history("crm", "ERROR", "crm blocked", 0, now);
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(latestError),
                List.of(history("crm", "SUCCESS", "crm recovered earlier", 1, now.minusSeconds(30))),
                List.of(latestError),
                List.of(
                        latestError,
                        history("crm", "ERROR", "crm timed out", 0, now.minusSeconds(10)),
                        history("crm", "SUCCESS", "crm recovered earlier", 1, now.minusSeconds(30))));

        McpToolServerHealth result = healthWithServers(serverRepository, syncService)
                .summarize();

        McpToolServerHealth.ServerHealth crm = result.servers().getFirst();
        assertEquals("UNHEALTHY", crm.healthStatus());
        assertEquals(McpServerHealthStatus.UNHEALTHY, result.highestHealthStatus());
        assertEquals(2, crm.consecutiveFailures());
        assertEquals(List.of(
                "Latest sync failed: crm blocked",
                "Server has 2 consecutive sync failures."),
                crm.issues());
        assertEquals(List.of(
                McpToolServerHealthIssues.ISSUE_SYNC_ERROR,
                McpToolServerHealthIssues.ISSUE_CONSECUTIVE_SYNC_FAILURES),
                crm.issueCodes());
        assertEquals(List.of(
                issueDetail(
                        McpToolServerHealthIssues.ISSUE_SYNC_ERROR,
                        McpIssueSeverity.CRITICAL,
                        "Latest sync failed: crm blocked"),
                issueDetail(
                        McpToolServerHealthIssues.ISSUE_CONSECUTIVE_SYNC_FAILURES,
                        McpIssueSeverity.CRITICAL,
                        "Server has 2 consecutive sync failures.")),
                crm.issueDetails());
        assertEquals(2, result.issueSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(McpIssueSeverity.CRITICAL, result.highestIssueSeverity());
        assertEquals(2, crm.issueSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(McpIssueSeverity.CRITICAL, crm.highestIssueSeverity());
    }

    @Test
    void summarizeCanFilterByServerName() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                healthServer("tenant-1", "docs", "http", "http://docs.local/mcp", true),
                healthServer("tenant-1", "crm", "http", "http://crm.local/mcp", true)));

        McpToolServerHealth result = healthWithServers(
                serverRepository,
                new McpToolDiscoverySyncServiceTestDouble(List.of(), List.of(), List.of()))
                .summarizeServer("crm");

        assertEquals(1, result.totalServers());
        assertEquals("crm", result.servers().getFirst().serverName());
        assertEquals("UNSYNCED", result.servers().getFirst().healthStatus());
    }

    @Test
    void summarizeCanFilterByEnabledHealthStatusAndSyncDue() {
        Instant now = Instant.now();
        McpServerRegistry due = healthServer("tenant-1", "due", "http", "http://due.local/mcp", true);
        due.setLastSyncAt(now.minus(Duration.ofMinutes(10)));
        McpServerRegistry current = healthServer("tenant-1", "current", "http", "http://current.local/mcp", true);
        current.setLastSyncAt(now);
        McpServerRegistry disabled = healthServer("tenant-1", "disabled", "http", "http://disabled.local/mcp", false);
        disabled.setLastSyncAt(now);
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(due, current, disabled));
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(
                        history("due", "SUCCESS", "due synced", 1, now),
                        history("current", "SUCCESS", "current synced", 1, now),
                        history("disabled", "SUCCESS", "disabled synced", 1, now)),
                List.of(
                        history("due", "SUCCESS", "due synced", 1, now),
                        history("current", "SUCCESS", "current synced", 1, now),
                        history("disabled", "SUCCESS", "disabled synced", 1, now)),
                List.of());

        McpToolServerHealthServiceTestHarness health = healthWithServers(serverRepository, syncService);

        McpToolServerHealth dueHealth = health.summarize(filters()
                .withEnabled(true)
                .withHealthStatus("HEALTHY")
                .withSyncDue(true)
                .build());
        McpToolServerHealth disabledHealth = health.summarize(filters()
                .withEnabled(false)
                .withHealthStatus("DISABLED")
                .build());
        McpToolServerHealth issueHealth = health.summarize(filters()
                .withIssues(true)
                .build());
        McpToolServerHealth clearHealth = health.summarize(filters()
                .withIssues(false)
                .build());
        McpToolServerHealth issueCodeHealth = health.summarize(filters()
                .withIssueCode("server-disabled")
                .build());
        McpToolServerHealth issueSeverityHealth = health.summarize(filters()
                .withIssueSeverity("info")
                .build());

        assertEquals(1, dueHealth.totalServers());
        assertEquals("due", dueHealth.servers().getFirst().serverName());
        assertEquals(true, dueHealth.servers().getFirst().syncDue());
        assertEquals("HEALTHY", dueHealth.servers().getFirst().healthStatus());

        assertEquals(1, disabledHealth.totalServers());
        assertEquals("disabled", disabledHealth.servers().getFirst().serverName());
        assertEquals(false, disabledHealth.servers().getFirst().enabled());
        assertEquals("DISABLED", disabledHealth.servers().getFirst().healthStatus());

        assertEquals(1, issueHealth.totalServers());
        assertEquals("disabled", issueHealth.servers().getFirst().serverName());
        assertEquals(List.of("Server is disabled."), issueHealth.servers().getFirst().issues());
        assertEquals(
                List.of(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED),
                issueHealth.servers().getFirst().issueCodes());
        assertEquals(
                List.of(issueDetail(
                        McpToolServerHealthIssues.ISSUE_SERVER_DISABLED,
                        McpIssueSeverity.INFO,
                        "Server is disabled.")),
                issueHealth.servers().getFirst().issueDetails());

        assertEquals(2, clearHealth.totalServers());
        assertEquals(List.of("current", "due"), clearHealth.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .sorted()
                .toList());

        assertEquals(1, issueCodeHealth.totalServers());
        assertEquals("disabled", issueCodeHealth.servers().getFirst().serverName());
        assertEquals(
                List.of(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED),
                issueCodeHealth.servers().getFirst().issueCodes());

        assertEquals(1, issueSeverityHealth.totalServers());
        assertEquals("disabled", issueSeverityHealth.servers().getFirst().serverName());
        assertEquals(
                1,
                issueSeverityHealth.issueSeverityCounts()
                        .get(McpIssueSeverity.INFO));
    }

    @Test
    void summarizeCanFilterByToolLifecycleSignals() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                healthServer("tenant-1", "paused", "http", "http://paused.local/mcp", true),
                healthServer("tenant-1", "archive", "http", "http://archive.local/mcp", true),
                healthServer("tenant-1", "clean", "http", "http://clean.local/mcp", true)));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(
                tool("tenant-1", "paused.search", false, Set.of("search", "mcp:paused"),
                        Set.of(McpToolLifecycle.SERVER_DISABLED_TAG)),
                tool("tenant-1", "archive.old", false, Set.of("search", "stale", "mcp:archive"),
                        Set.of(McpToolLifecycle.STALE_TAG, McpToolLifecycle.SERVER_RETIRED_TAG)),
                tool("tenant-1", "clean.search", true, Set.of("search", "mcp:clean"), Set.of())));
        McpToolDiscoverySyncServiceTestDouble syncService = new McpToolDiscoverySyncServiceTestDouble(
                List.of(
                        history("paused", "SUCCESS", "paused synced", 1, now),
                        history("archive", "SUCCESS", "archive synced", 1, now),
                        history("clean", "SUCCESS", "clean synced", 1, now)),
                List.of(
                        history("paused", "SUCCESS", "paused synced", 1, now),
                        history("archive", "SUCCESS", "archive synced", 1, now),
                        history("clean", "SUCCESS", "clean synced", 1, now)),
                List.of());

        McpToolServerHealthServiceTestHarness health = healthWith(serverRepository, toolRepository, syncService);

        McpToolServerHealth serverDisabledHealth = health.summarize(filters()
                .withServerDisabledTools(true)
                .build());
        McpToolServerHealth retiredHealth = health.summarize(filters()
                .withStaleTools(true)
                .withRetiredTools(true)
                .withLifecycleState("retired")
                .build());
        McpToolServerHealth activeHealth = health.summarize(filters()
                .withLifecycleState("active")
                .build());

        assertEquals(1, serverDisabledHealth.totalServers());
        assertEquals("paused", serverDisabledHealth.servers().getFirst().serverName());
        assertEquals(1, serverDisabledHealth.servers().getFirst().serverDisabledTools());

        assertEquals(1, retiredHealth.totalServers());
        assertEquals("archive", retiredHealth.servers().getFirst().serverName());
        assertEquals(1, retiredHealth.servers().getFirst().staleTools());
        assertEquals(1, retiredHealth.servers().getFirst().retiredTools());

        assertEquals(1, activeHealth.totalServers());
        assertEquals("clean", activeHealth.servers().getFirst().serverName());
        assertEquals(1, activeHealth.servers().getFirst().activeTools());
    }

    @Test
    void summarizeReportsUnavailableRegistryMode() {
        McpToolServerHealth result = disabledRegistryHealth().summarize();

        assertEquals(0, result.totalServers());
        assertEquals(List.of("MCP server health is unavailable: MCP registry database mode is not enabled."),
                result.warnings());
    }

}
