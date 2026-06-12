package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunPreview;

/**
 * Plain-text renderer for run specification validation results.
 */
final class WayangSpecTextFormat {

    private WayangSpecTextFormat() {
    }

    static String validationText(String path, AgentRunPreview preview) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run spec").append(System.lineSeparator());
        output.append("path: ").append(path).append(System.lineSeparator());
        output.append("surface: ").append(preview.surfaceId()).append(System.lineSeparator());
        output.append("ready: ").append(CliText.yesNo(preview.ready())).append(System.lineSeparator());
        output.append("prompt chars: ").append(preview.promptCharacters()).append(System.lineSeparator());
        output.append("workspace attached: ")
                .append(CliText.yesNo(preview.workspaceAttached()))
                .append(System.lineSeparator());
        output.append("harness attached: ")
                .append(CliText.yesNo(preview.harnessAttached()))
                .append(System.lineSeparator());
        CliText.appendBulletBlock(output, "missing context", preview.surfacePolicyAssessment().missingContextKeys());
        CliText.appendBulletBlock(output, "recommendations", preview.surfacePolicyAssessment().recommendations());
        return output.toString();
    }
}
