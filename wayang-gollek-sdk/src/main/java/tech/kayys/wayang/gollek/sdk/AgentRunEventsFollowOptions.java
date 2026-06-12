package tech.kayys.wayang.gollek.sdk;

public record AgentRunEventsFollowOptions(
        AgentRunEventsQuery query,
        int maxPolls,
        long pollMillis) {

    public static final int DEFAULT_MAX_POLLS = 60;
    public static final long DEFAULT_POLL_MILLIS = 1_000;

    public AgentRunEventsFollowOptions {
        query = query == null ? AgentRunEventsQuery.all() : query;
        maxPolls = Math.max(1, maxPolls);
        pollMillis = Math.max(1, pollMillis);
    }

    public static AgentRunEventsFollowOptions defaults() {
        return new AgentRunEventsFollowOptions(
                AgentRunEventsQuery.all(),
                DEFAULT_MAX_POLLS,
                DEFAULT_POLL_MILLIS);
    }

    public static AgentRunEventsFollowOptions of(
            AgentRunEventsQuery query,
            Integer maxPolls,
            Long pollMillis) {
        return new AgentRunEventsFollowOptions(
                query,
                maxPolls == null ? DEFAULT_MAX_POLLS : maxPolls,
                pollMillis == null ? DEFAULT_POLL_MILLIS : pollMillis);
    }
}
