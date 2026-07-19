package tech.kayys.wayang.agent.history;

public record AgentRunHistoryPage(
        int totalRuns,
        int returnedRuns,
        int pageSize,
        int offset) {

    public AgentRunHistoryPage {
        totalRuns = Math.max(0, totalRuns);
        returnedRuns = Math.max(0, returnedRuns);
        pageSize = pageSize <= 0 ? AgentRunHistoryQuery.DEFAULT_LIMIT : pageSize;
        offset = Math.max(0, offset);
        if (returnedRuns > 0) {
            totalRuns = Math.max(totalRuns, offset + returnedRuns);
        }
    }

    public int windowStart() {
        return returnedRuns == 0 ? 0 : offset + 1;
    }

    public int windowEnd() {
        return returnedRuns == 0 ? 0 : offset + returnedRuns;
    }

    public int previousOffset() {
        if (offset >= totalRuns && totalRuns > 0) {
            return ((totalRuns - 1) / pageSize) * pageSize;
        }
        return Math.max(0, offset - pageSize);
    }

    public boolean hasPrevious() {
        return offset > 0;
    }

    public int nextOffset() {
        return Math.min(totalRuns, offset + returnedRuns);
    }

    public boolean hasMore() {
        return nextOffset() < totalRuns;
    }

    public boolean truncated() {
        return hasPrevious() || hasMore();
    }

    public boolean empty() {
        return returnedRuns == 0;
    }
}
