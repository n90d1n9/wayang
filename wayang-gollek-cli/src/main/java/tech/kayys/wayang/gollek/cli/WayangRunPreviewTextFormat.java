package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunPreview;

/**
 * Plain-text renderer for dry-run previews of an agent run request.
 */
final class WayangRunPreviewTextFormat {

    private WayangRunPreviewTextFormat() {
    }

    static String text(AgentRunPreview preview) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run preview").append(System.lineSeparator());
        output.append("request: ").append(preview.requestId()).append(System.lineSeparator());
        output.append("surface: ").append(preview.surfaceId()).append(System.lineSeparator());
        output.append("ready: ").append(CliText.yesNo(preview.ready())).append(System.lineSeparator());
        output.append("tenant: ").append(preview.tenantId()).append(System.lineSeparator());
        output.append("model: ")
                .append(CliText.trimToDefault(preview.modelId(), "backend-default"))
                .append(System.lineSeparator());
        output.append("workflow: ").append(preview.workflowId()).append(System.lineSeparator());
        if (!preview.sessionId().isBlank()) {
            output.append("session: ").append(preview.sessionId()).append(System.lineSeparator());
        }
        if (!preview.userId().isBlank()) {
            output.append("user: ").append(preview.userId()).append(System.lineSeparator());
        }
        output.append("prompt chars: ").append(preview.promptCharacters()).append(System.lineSeparator());
        output.append("system prompt: ")
                .append(CliText.yesNo(preview.systemPromptPresent()))
                .append(System.lineSeparator());
        output.append("memory: ").append(preview.memoryEnabled() ? "enabled" : "disabled").append(System.lineSeparator());
        output.append("max steps: ").append(preview.maxSteps()).append(System.lineSeparator());
        output.append("workspace attached: ")
                .append(CliText.yesNo(preview.workspaceAttached()))
                .append(System.lineSeparator());
        output.append("harness attached: ")
                .append(CliText.yesNo(preview.harnessAttached()))
                .append(System.lineSeparator());
        CliText.appendBulletBlock(output, "skills", preview.skills());
        CliText.appendBulletBlock(output, "context keys", preview.contextKeys());
        CliText.appendBulletBlock(output, "missing context", preview.surfacePolicyAssessment().missingContextKeys());
        CliText.appendBulletBlock(output, "recommendations", preview.surfacePolicyAssessment().recommendations());
        CliText.appendBulletBlock(output, "resolved skills", preview.skillAssessment().resolvedSkillIds());
        WayangRunReadinessFormat.appendSkillIssues(output, preview.skillAssessment());
        return output.toString();
    }
}
