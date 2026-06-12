package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.QueryParam;

public class McpServerHealthQuery {

    @QueryParam("serverName")
    String serverName;

    @QueryParam("enabled")
    Boolean enabled;

    @QueryParam("healthStatus")
    String healthStatus;

    @QueryParam("syncDue")
    Boolean syncDue;

    @QueryParam("hasIssues")
    Boolean hasIssues;

    @QueryParam("issueCode")
    String issueCode;

    @QueryParam("issueSeverity")
    String issueSeverity;

    @QueryParam("minIssueSeverity")
    String minIssueSeverity;

    @QueryParam("hasStaleTools")
    Boolean hasStaleTools;

    @QueryParam("hasServerDisabledTools")
    Boolean hasServerDisabledTools;

    @QueryParam("hasRetiredTools")
    Boolean hasRetiredTools;

    @QueryParam("lifecycleState")
    String lifecycleState;

    @QueryParam("minHealthStatus")
    String minHealthStatus;

    @QueryParam("attentionRequired")
    Boolean attentionRequired;

    @QueryParam("actionCode")
    String actionCode;

    @QueryParam("actionSeverity")
    String actionSeverity;

    @QueryParam("minActionSeverity")
    String minActionSeverity;

    @QueryParam("actionSafeToAutomate")
    Boolean actionSafeToAutomate;

    @QueryParam("actionQueueLimit")
    Integer actionQueueLimit;

    @QueryParam("actionQueueOffset")
    Integer actionQueueOffset;

    @QueryParam("actionCallable")
    Boolean actionCallable;

    @QueryParam("actionMethod")
    String actionMethod;

    @QueryParam("actionPath")
    String actionPath;

    @QueryParam("actionExecutionMode")
    String actionExecutionMode;

    static McpServerHealthQuery of(
            String serverName,
            Boolean enabled,
            String healthStatus,
            Boolean syncDue,
            Boolean hasIssues,
            String issueCode,
            String issueSeverity,
            String minIssueSeverity,
            Boolean hasStaleTools,
            Boolean hasServerDisabledTools,
            Boolean hasRetiredTools,
            String lifecycleState,
            String minHealthStatus,
            Boolean attentionRequired,
            String actionCode,
            String actionSeverity,
            String minActionSeverity,
            Boolean actionSafeToAutomate,
            Integer actionQueueLimit,
            Integer actionQueueOffset,
            Boolean actionCallable,
            String actionMethod,
            String actionPath,
            String actionExecutionMode) {
        McpServerHealthQuery query = new McpServerHealthQuery();
        query.serverName = serverName;
        query.enabled = enabled;
        query.healthStatus = healthStatus;
        query.syncDue = syncDue;
        query.hasIssues = hasIssues;
        query.issueCode = issueCode;
        query.issueSeverity = issueSeverity;
        query.minIssueSeverity = minIssueSeverity;
        query.hasStaleTools = hasStaleTools;
        query.hasServerDisabledTools = hasServerDisabledTools;
        query.hasRetiredTools = hasRetiredTools;
        query.lifecycleState = lifecycleState;
        query.minHealthStatus = minHealthStatus;
        query.attentionRequired = attentionRequired;
        query.actionCode = actionCode;
        query.actionSeverity = actionSeverity;
        query.minActionSeverity = minActionSeverity;
        query.actionSafeToAutomate = actionSafeToAutomate;
        query.actionQueueLimit = actionQueueLimit;
        query.actionQueueOffset = actionQueueOffset;
        query.actionCallable = actionCallable;
        query.actionMethod = actionMethod;
        query.actionPath = actionPath;
        query.actionExecutionMode = actionExecutionMode;
        return query;
    }

    McpServerHealthFilters toFilters() {
        return McpServerHealthFilters.builder()
                .withServerName(serverName)
                .withEnabled(enabled)
                .withHealthStatus(healthStatus)
                .withSyncDue(syncDue)
                .withIssues(hasIssues)
                .withIssueCode(issueCode)
                .withIssueSeverity(issueSeverity)
                .withMinIssueSeverity(minIssueSeverity)
                .withStaleTools(hasStaleTools)
                .withServerDisabledTools(hasServerDisabledTools)
                .withRetiredTools(hasRetiredTools)
                .withLifecycleState(lifecycleState)
                .withMinHealthStatus(minHealthStatus)
                .withAttentionRequired(attentionRequired)
                .withActionCode(actionCode)
                .withActionSeverity(actionSeverity)
                .withMinActionSeverity(minActionSeverity)
                .withActionSafeToAutomate(actionSafeToAutomate)
                .withActionQueueWindow(actionQueueOffset, actionQueueLimit)
                .withActionCallable(actionCallable)
                .withActionMethod(actionMethod)
                .withActionPath(actionPath)
                .withActionExecutionMode(actionExecutionMode)
                .build();
    }
}
