package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;

final class WayangRunControlTextFormat {

    private WayangRunControlTextFormat() {
    }

    static String forgetText(AgentRunForgetResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(result.runId()).append(System.lineSeparator());
        output.append("forgotten: ").append(result.forgotten()).append(System.lineSeparator());
        output.append("outcome: ").append(result.outcome()).append(System.lineSeparator());
        if (!result.message().isBlank()) {
            output.append("message: ").append(result.message()).append(System.lineSeparator());
        }
        if (!result.metadata().isEmpty()) {
            output.append("metadata: ").append(result.metadata()).append(System.lineSeparator());
        }
        return output.toString();
    }

    static String cancelText(AgentRunCancelResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(result.runId()).append(System.lineSeparator());
        output.append("cancelled: ").append(result.cancelled()).append(System.lineSeparator());
        output.append("outcome: ").append(result.outcome()).append(System.lineSeparator());
        output.append("state: ").append(result.handle().state().name().toLowerCase()).append(System.lineSeparator());
        output.append("strategy: ").append(result.handle().strategy()).append(System.lineSeparator());
        if (!result.message().isBlank()) {
            output.append("message: ").append(result.message()).append(System.lineSeparator());
        }
        if (!result.metadata().isEmpty()) {
            output.append("metadata: ").append(result.metadata()).append(System.lineSeparator());
        }
        return output.toString();
    }

    static String waitText(AgentRunWaitResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(result.runId()).append(System.lineSeparator());
        output.append("outcome: ").append(result.outcome()).append(System.lineSeparator());
        output.append("terminal: ").append(result.terminal()).append(System.lineSeparator());
        output.append("timedOut: ").append(result.timedOut()).append(System.lineSeparator());
        output.append("attempts: ").append(result.attempts()).append(System.lineSeparator());
        output.append("elapsedMillis: ").append(result.elapsedMillis()).append(System.lineSeparator());
        output.append("state: ").append(result.status().handle().state().name().toLowerCase()).append(System.lineSeparator());
        output.append("known: ").append(result.status().known()).append(System.lineSeparator());
        if (!result.message().isBlank()) {
            output.append("message: ").append(result.message()).append(System.lineSeparator());
        }
        if (!result.metadata().isEmpty()) {
            output.append("metadata: ").append(result.metadata()).append(System.lineSeparator());
        }
        return output.toString();
    }
}
