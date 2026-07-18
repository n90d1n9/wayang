package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.ComponentStatus;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;

final class WayangStatusTextFormat {

    private WayangStatusTextFormat() {
    }

    static String text(WayangPlatformStatus status) {
        StringBuilder output = new StringBuilder();
        output.append(status.productName())
                .append(" ")
                .append(status.version())
                .append(System.lineSeparator());
        output.append("Boundary: Wayang is the agentic platform; Gollek and Gamelan stay external engines.")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        append(output, status.gollek());
        append(output, status.gamelan());
        append(output, status.agentCore());
        append(output, status.rag());
        append(output, status.mcp());
        output.append("Skills: ").append(status.activeSkills()).append(" active").append(System.lineSeparator());
        if (!status.notes().isEmpty()) {
            output.append(System.lineSeparator()).append("Notes:").append(System.lineSeparator());
            status.notes().forEach(note -> output.append("- ").append(note).append(System.lineSeparator()));
        }
        return output.toString();
    }

    private static void append(StringBuilder output, ComponentStatus component) {
        output.append(component.name())
                .append(": ")
                .append(component.state())
                .append(" - ")
                .append(component.role());
        if (!component.endpoint().isBlank()) {
            output.append(" [").append(component.endpoint()).append("]");
        }
        output.append(System.lineSeparator());
    }
}
