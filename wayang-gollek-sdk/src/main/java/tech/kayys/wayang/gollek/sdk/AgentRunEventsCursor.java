package tech.kayys.wayang.gollek.sdk;

public record AgentRunEventsCursor(
        long afterSequence,
        long firstSequence,
        long lastSequence,
        long nextAfterSequence,
        int limit,
        int totalEvents,
        int returnedEvents) {

    public AgentRunEventsCursor {
        afterSequence = Math.max(0, afterSequence);
        returnedEvents = Math.max(0, returnedEvents);
        totalEvents = Math.max(Math.max(0, totalEvents), returnedEvents);
        if (returnedEvents == 0) {
            firstSequence = 0;
            lastSequence = 0;
        } else {
            firstSequence = Math.max(0, firstSequence);
            lastSequence = Math.max(firstSequence, lastSequence);
        }
        nextAfterSequence = Math.max(afterSequence, Math.max(lastSequence, nextAfterSequence));
        if (limit <= 0) {
            limit = AgentRunEventsQuery.DEFAULT_LIMIT;
        }
        limit = Math.min(limit, AgentRunEventsQuery.MAX_LIMIT);
    }

    public int remainingEvents() {
        return Math.max(0, totalEvents - returnedEvents);
    }

    public boolean advanced() {
        return nextAfterSequence > afterSequence;
    }

    public boolean truncated() {
        return remainingEvents() > 0;
    }

    public boolean empty() {
        return returnedEvents == 0;
    }
}
