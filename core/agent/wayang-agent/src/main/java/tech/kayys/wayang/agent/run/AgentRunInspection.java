package tech.kayys.wayang.agent.run;

import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.event.AgentRunEventsQuery;
import tech.kayys.wayang.client.SdkText;

public record AgentRunInspection(
        String runId,
        AgentRunStatus status,
        AgentRunEvents events,
        String message) {

    public AgentRunInspection {
        runId = SdkText.trimToEmpty(runId);
        status = status == null
                ? AgentRunStatus.unknown(runId, "Run status is unknown.")
                : status;
        events = events == null
                ? new AgentRunEvents(runId, AgentRunEventsQuery.all(), java.util.List.of(), 0, "")
                : events;
        message = SdkText.trimToEmpty(message);
    }

    public boolean known() {
        return status.known();
    }

    public boolean empty() {
        return !known() && events.empty();
    }

    public String outcome() {
        if (known()) {
            return status.outcome();
        }
        return events.empty() ? AgentRunOutcomes.EMPTY : events.outcome();
    }
}
