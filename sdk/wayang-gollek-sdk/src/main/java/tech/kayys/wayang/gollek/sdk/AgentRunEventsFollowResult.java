package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record AgentRunEventsFollowResult(
        String runId,
        AgentRunEventsQuery initialQuery,
        AgentRunEventsQuery nextQuery,
        AgentRunEvents lastEvents,
        boolean terminal,
        boolean maxPollsReached,
        int polls,
        long elapsedMillis,
        String message,
        Map<String, Object> metadata) {

    public AgentRunEventsFollowResult {
        initialQuery = initialQuery == null ? AgentRunEventsQuery.all() : initialQuery;
        nextQuery = nextQuery == null ? initialQuery : nextQuery;
        lastEvents = lastEvents == null
                ? new AgentRunEvents(runId, initialQuery, List.of(), 0, "")
                : lastEvents;
        runId = SdkText.trimToDefault(runId, lastEvents.runId());
        terminal = lastEvents.events().stream()
                .anyMatch(event -> event.state().terminal());
        maxPollsReached = maxPollsReached && !terminal;
        polls = Math.max(0, polls);
        elapsedMillis = Math.max(0, elapsedMillis);
        message = SdkText.trimToEmpty(message);
        metadata = SdkMaps.copy(metadata);
    }

    public boolean successful() {
        return terminal;
    }

    public String outcome() {
        if (terminal) {
            return AgentRunOutcomes.TERMINAL;
        }
        if (maxPollsReached) {
            return AgentRunOutcomes.MAX_POLLS;
        }
        return empty() ? AgentRunOutcomes.EMPTY : AgentRunOutcomes.PENDING;
    }

    public boolean empty() {
        return lastEvents.empty();
    }

    public AgentRunEvent terminalEvent() {
        return lastEvents.events().stream()
                .filter(event -> event.state().terminal())
                .findFirst()
                .orElse(null);
    }

    public String terminalState() {
        AgentRunEvent event = terminalEvent();
        return event == null ? "" : AgentRunStates.wireName(event.state());
    }

    public String terminalEventType() {
        AgentRunEvent event = terminalEvent();
        return event == null ? "" : event.type();
    }

    public long terminalSequence() {
        AgentRunEvent event = terminalEvent();
        return event == null ? 0 : event.sequence();
    }

    public long nextAfterSequence() {
        return nextQuery.afterSequence();
    }
}
