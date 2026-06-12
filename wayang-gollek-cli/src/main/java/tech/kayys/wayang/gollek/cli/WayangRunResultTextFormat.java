package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunResult;

final class WayangRunResultTextFormat {

    private WayangRunResultTextFormat() {
    }

    static String text(AgentRunResult result) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(result.runId()).append(System.lineSeparator());
        output.append("strategy: ").append(result.strategy()).append(System.lineSeparator());
        output.append("state: ").append(result.handle().state().name().toLowerCase()).append(System.lineSeparator());
        output.append("status: ").append(result.successful() ? "success" : "failed").append(System.lineSeparator());
        output.append("outcome: ").append(result.outcome()).append(System.lineSeparator());
        output.append(System.lineSeparator());
        output.append(result.answer()).append(System.lineSeparator());
        output.append(System.lineSeparator());
        output.append("steps:").append(System.lineSeparator());
        for (String step : result.steps()) {
            output.append("- ").append(step).append(System.lineSeparator());
        }
        WayangRunReadinessFormat.appendMetadataReadiness(output, result.metadata());
        output.append("metadata: ").append(result.metadata()).append(System.lineSeparator());
        return output.toString();
    }
}
