package tech.kayys.wayang.gollek.sdk;

import java.util.Locale;

public record AgentRunEventsQuery(
        AgentRunState state,
        String type,
        long afterSequence,
        int limit) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public AgentRunEventsQuery {
        type = SdkText.trimToEmpty(type).toLowerCase(Locale.ROOT);
        afterSequence = Math.max(0, afterSequence);
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        limit = Math.min(limit, MAX_LIMIT);
    }

    public AgentRunEventsQuery(AgentRunState state, int limit) {
        this(state, "", 0, limit);
    }

    public AgentRunEventsQuery(AgentRunState state, String type, int limit) {
        this(state, type, 0, limit);
    }

    public static AgentRunEventsQuery all() {
        return new AgentRunEventsQuery(null, "", 0, DEFAULT_LIMIT);
    }

    public static AgentRunEventsQuery of(String state, String type, Integer limit) {
        return of(state, type, null, limit);
    }

    public static AgentRunEventsQuery of(String state, String type, Long afterSequence, Integer limit) {
        return new AgentRunEventsQuery(
                AgentRunStates.parseOptional(state),
                type,
                afterSequence == null ? 0 : afterSequence,
                limit == null ? DEFAULT_LIMIT : limit);
    }

    public boolean filtered() {
        return state != null || !type.isBlank() || afterSequence > 0;
    }

    public boolean matches(AgentRunEvent event) {
        if (event == null) {
            return false;
        }
        return event.sequence() > afterSequence
                && (state == null || event.state() == state)
                && (type.isBlank() || type.equals(SdkText.trimToEmpty(event.type()).toLowerCase(Locale.ROOT)));
    }
}
