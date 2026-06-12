package tech.kayys.wayang.tool.mcp;

record McpServerHealthIssueFilters(
        Boolean hasIssues,
        String issueCode,
        String issueSeverity,
        String minIssueSeverity) {

    static McpServerHealthIssueFilters from(McpServerHealthFilters filters) {
        return new McpServerHealthIssueFilters(
                filters.hasIssues(),
                filters.issueCode(),
                filters.issueSeverity(),
                filters.minIssueSeverity());
    }

    boolean matches(McpToolServerHealth.ServerHealth health) {
        return matchesHasIssues(health)
                && matchesIssueCode(health)
                && matchesIssueSeverity(health)
                && matchesMinIssueSeverity(health);
    }

    private boolean matchesHasIssues(McpToolServerHealth.ServerHealth health) {
        return hasIssues == null || (!health.issues().isEmpty()) == hasIssues;
    }

    private boolean matchesIssueCode(McpToolServerHealth.ServerHealth health) {
        return issueCode == null || health.issueCodes().contains(issueCode);
    }

    private boolean matchesIssueSeverity(McpToolServerHealth.ServerHealth health) {
        return issueSeverity == null || health.issueSeverityCounts().getOrDefault(issueSeverity, 0) > 0;
    }

    private boolean matchesMinIssueSeverity(McpToolServerHealth.ServerHealth health) {
        if (minIssueSeverity == null) {
            return true;
        }
        int requiredRank = McpIssueSeverity.rank(minIssueSeverity);
        return requiredRank > 0
                && McpIssueSeverity.rank(health.highestIssueSeverity()) >= requiredRank;
    }
}
