package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunReadiness;

/**
 * Plain-text renderer for agent run readiness preflight checks.
 */
final class WayangRunPreflightTextFormat {

    private WayangRunPreflightTextFormat() {
    }

    static String text(AgentRunReadiness readiness) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang run preflight").append(System.lineSeparator());
        output.append("surface: ").append(readiness.surfaceId()).append(System.lineSeparator());
        WayangRunReadinessFormat.appendSummary(output, "ready", readiness);
        CliText.appendBulletBlock(output, "satisfied context", readiness.surfacePolicyAssessment().satisfiedContextKeys());
        CliText.appendBulletBlock(output, "missing context", readiness.surfacePolicyAssessment().missingContextKeys());
        CliText.appendBulletBlock(output, "recommendations", readiness.surfacePolicyAssessment().recommendations());
        CliText.appendBulletBlock(output, "routing hints", readiness.surfacePolicyAssessment().routingHints());
        CliText.appendBulletBlock(output, "requested skills", readiness.skillAssessment().requestedSkills());
        CliText.appendBulletBlock(output, "resolved skills", readiness.skillAssessment().resolvedSkillIds());
        WayangRunReadinessFormat.appendSkillIssues(output, readiness.skillAssessment());
        return output.toString();
    }
}
