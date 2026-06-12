package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

final class WayangRunInspectionTextFormat {

    private WayangRunInspectionTextFormat() {
    }

    static String statusText(AgentRunStatus status) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run ").append(status.handle().runId()).append(System.lineSeparator());
        output.append("state: ").append(status.handle().state().name().toLowerCase()).append(System.lineSeparator());
        output.append("outcome: ").append(status.outcome()).append(System.lineSeparator());
        output.append("known: ").append(status.known()).append(System.lineSeparator());
        output.append("strategy: ").append(status.handle().strategy()).append(System.lineSeparator());
        if (!status.message().isBlank()) {
            output.append(System.lineSeparator()).append(status.message()).append(System.lineSeparator());
        }
        if (!status.metadata().isEmpty()) {
            output.append("metadata: ").append(status.metadata()).append(System.lineSeparator());
        }
        return output.toString();
    }

    static String inspectionText(AgentRunInspection inspection) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run inspection ").append(inspection.runId()).append(System.lineSeparator());
        output.append("outcome: ").append(inspection.outcome()).append(System.lineSeparator());
        output.append("known: ").append(inspection.known()).append(System.lineSeparator());
        if (!inspection.message().isBlank()) {
            output.append("message: ").append(inspection.message()).append(System.lineSeparator());
        }
        output.append(System.lineSeparator()).append("status:").append(System.lineSeparator());
        output.append(statusText(inspection.status()));
        output.append(System.lineSeparator()).append("events:").append(System.lineSeparator());
        output.append(WayangRunEventTextFormat.eventsText(inspection.events()));
        return output.toString();
    }
}
