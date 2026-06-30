package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunStates;

final class WayangRunEventTextFormat {

    private WayangRunEventTextFormat() {
    }

    static String eventsText(AgentRunEvents events) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(events.runId()).append(System.lineSeparator());
        output.append("outcome: ").append(events.outcome()).append(System.lineSeparator());
        output.append("cursor: ")
                .append(events.cursor().afterSequence())
                .append("->")
                .append(events.cursor().nextAfterSequence())
                .append(System.lineSeparator());
        output.append("totalEvents: ").append(events.totalEvents()).append(System.lineSeparator());
        output.append("returnedEvents: ").append(events.returnedEvents()).append(System.lineSeparator());
        output.append("firstSequence: ").append(events.firstSequence()).append(System.lineSeparator());
        output.append("lastSequence: ").append(events.lastSequence()).append(System.lineSeparator());
        output.append("nextAfterSequence: ").append(events.nextAfterSequence()).append(System.lineSeparator());
        output.append("remainingEvents: ").append(events.cursor().remainingEvents()).append(System.lineSeparator());
        output.append("advanced: ").append(events.cursor().advanced()).append(System.lineSeparator());
        output.append("truncated: ").append(events.truncated()).append(System.lineSeparator());
        output.append("summary: ").append(events.summary()).append(System.lineSeparator());
        output.append("stateCounts: ").append(events.stateCounts()).append(System.lineSeparator());
        output.append("typeCounts: ").append(events.typeCounts()).append(System.lineSeparator());
        output.append("stateSummaries: ").append(events.stateSummaries()).append(System.lineSeparator());
        output.append("typeSummaries: ").append(events.typeSummaries()).append(System.lineSeparator());
        output.append("state: ").append(events.query().state() == null
                ? "all"
                : AgentRunStates.wireName(events.query().state())).append(System.lineSeparator());
        output.append("type: ").append(events.query().type().isBlank() ? "all" : events.query().type())
                .append(System.lineSeparator());
        output.append("afterSequence: ").append(events.query().afterSequence()).append(System.lineSeparator());
        output.append("limit: ").append(events.query().limit()).append(System.lineSeparator());
        if (!events.message().isBlank()) {
            output.append("message: ").append(events.message()).append(System.lineSeparator());
        }
        if (!events.events().isEmpty()) {
            output.append(System.lineSeparator()).append("events:").append(System.lineSeparator());
            for (AgentRunEvent event : events.events()) {
                output.append("- #")
                        .append(event.sequence())
                        .append(" ")
                        .append(event.type())
                        .append(" ")
                        .append(event.state().name().toLowerCase())
                        .append(" ")
                        .append(event.message())
                        .append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    static String eventsStatsText(AgentRunEvents events) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run event stats ").append(events.runId()).append(System.lineSeparator());
        output.append("cursor: ")
                .append(events.cursor().afterSequence())
                .append("->")
                .append(events.cursor().nextAfterSequence())
                .append(System.lineSeparator());
        output.append("totalEvents: ").append(events.totalEvents()).append(System.lineSeparator());
        output.append("returnedEvents: ").append(events.returnedEvents()).append(System.lineSeparator());
        output.append("summary: ").append(events.summary()).append(System.lineSeparator());
        output.append("stateCounts: ").append(events.stateCounts()).append(System.lineSeparator());
        output.append("typeCounts: ").append(events.typeCounts()).append(System.lineSeparator());
        output.append("stateSummaries: ").append(events.stateSummaries()).append(System.lineSeparator());
        output.append("typeSummaries: ").append(events.typeSummaries()).append(System.lineSeparator());
        if (!events.message().isBlank()) {
            output.append("message: ").append(events.message()).append(System.lineSeparator());
        }
        return output.toString();
    }
}
