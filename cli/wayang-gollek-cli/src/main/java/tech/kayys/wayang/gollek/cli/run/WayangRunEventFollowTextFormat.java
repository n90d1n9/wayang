package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunEventsFollowResult;

final class WayangRunEventFollowTextFormat {

    private WayangRunEventFollowTextFormat() {
    }

    static String text(AgentRunEventsFollowResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run event follow ").append(result.runId()).append(System.lineSeparator());
        output.append("successful: ").append(result.successful()).append(System.lineSeparator());
        output.append("outcome: ").append(result.outcome()).append(System.lineSeparator());
        output.append("terminal: ").append(result.terminal()).append(System.lineSeparator());
        if (!result.terminalState().isBlank()) {
            output.append("terminalState: ").append(result.terminalState()).append(System.lineSeparator());
        }
        if (!result.terminalEventType().isBlank()) {
            output.append("terminalEventType: ").append(result.terminalEventType()).append(System.lineSeparator());
        }
        if (result.terminalSequence() > 0) {
            output.append("terminalSequence: ").append(result.terminalSequence()).append(System.lineSeparator());
        }
        output.append("maxPollsReached: ").append(result.maxPollsReached()).append(System.lineSeparator());
        output.append("polls: ").append(result.polls()).append(System.lineSeparator());
        output.append("elapsedMillis: ").append(result.elapsedMillis()).append(System.lineSeparator());
        output.append("nextAfterSequence: ").append(result.nextAfterSequence()).append(System.lineSeparator());
        output.append("lastReturnedEvents: ").append(result.lastEvents().returnedEvents()).append(System.lineSeparator());
        output.append("lastCursor: ")
                .append(result.lastEvents().cursor().afterSequence())
                .append("->")
                .append(result.lastEvents().cursor().nextAfterSequence())
                .append(System.lineSeparator());
        if (!result.message().isBlank()) {
            output.append("message: ").append(result.message()).append(System.lineSeparator());
        }
        if (!result.metadata().isEmpty()) {
            output.append("metadata: ").append(result.metadata()).append(System.lineSeparator());
        }
        return output.toString();
    }
}
