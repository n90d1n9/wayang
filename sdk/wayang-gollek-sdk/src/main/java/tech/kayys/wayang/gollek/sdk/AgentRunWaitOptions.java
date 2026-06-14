package tech.kayys.wayang.gollek.sdk;

public record AgentRunWaitOptions(
        long timeoutMillis,
        long pollMillis) {

    public static final long DEFAULT_TIMEOUT_MILLIS = 30_000;
    public static final long DEFAULT_POLL_MILLIS = 1_000;
    public static final long MAX_TIMEOUT_MILLIS = 3_600_000;

    public AgentRunWaitOptions {
        timeoutMillis = Math.max(0, Math.min(timeoutMillis, MAX_TIMEOUT_MILLIS));
        pollMillis = Math.max(1, pollMillis);
    }

    public static AgentRunWaitOptions defaults() {
        return new AgentRunWaitOptions(DEFAULT_TIMEOUT_MILLIS, DEFAULT_POLL_MILLIS);
    }

    public static AgentRunWaitOptions of(Integer timeoutSeconds, Integer pollMillis) {
        long timeout = timeoutSeconds == null
                ? DEFAULT_TIMEOUT_MILLIS
                : Math.max(0, timeoutSeconds.longValue()) * 1_000;
        long poll = pollMillis == null ? DEFAULT_POLL_MILLIS : pollMillis.longValue();
        return new AgentRunWaitOptions(timeout, poll);
    }
}
