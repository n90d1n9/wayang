package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunReadiness;
import tech.kayys.wayang.gollek.sdk.AgentRunSkillAssessment;

import java.util.List;
import java.util.Map;

/**
 * Shared text sections for agent run readiness summaries, preflights, and metadata views.
 */
final class WayangRunReadinessFormat {

    private WayangRunReadinessFormat() {
    }

    static void appendSummary(StringBuilder output, String readyLabel, AgentRunReadiness readiness) {
        output.append(readyLabel)
                .append(": ")
                .append(CliText.yesNo(readiness.ready()))
                .append(System.lineSeparator());
        output.append("surface ready: ")
                .append(CliText.yesNo(readiness.surfacePolicyAssessment().ready()))
                .append(System.lineSeparator());
        output.append("skills ready: ")
                .append(CliText.yesNo(readiness.skillAssessment().ready()))
                .append(System.lineSeparator());
    }

    static void appendSkillIssues(StringBuilder output, AgentRunSkillAssessment assessment) {
        CliText.appendBulletBlockIfAny(output, "unknown skills", assessment.unknownSkills());
        CliText.appendBulletBlockIfAny(output, "unavailable skills", assessment.unavailableSkillIds());
        CliText.appendBulletBlockIfAny(output, "surface-incompatible skills", assessment.incompatibleSkillIds());
        CliText.appendBulletBlockIfAny(output, "skill recommendations", assessment.recommendations());
    }

    static void appendMetadataReadiness(StringBuilder output, Map<String, Object> metadata) {
        Map<String, Object> readiness = map(metadata.get("runReadiness"));
        if (readiness.isEmpty()) {
            return;
        }
        output.append("readiness: ")
                .append(CliText.yesNo(readiness.get("ready")))
                .append(System.lineSeparator());
        Map<String, Object> surface = map(readiness.get("surfacePolicyAssessment"));
        if (!surface.isEmpty()) {
            output.append("surface ready: ")
                    .append(CliText.yesNo(surface.get("ready")))
                    .append(System.lineSeparator());
        }
        Map<String, Object> skills = map(readiness.get("skillAssessment"));
        if (!skills.isEmpty()) {
            output.append("skills ready: ")
                    .append(CliText.yesNo(skills.get("ready")))
                    .append(System.lineSeparator());
            CliText.appendBulletBlockIfAny(output, "unknown skills", list(skills.get("unknownSkills")));
            CliText.appendBulletBlockIfAny(output, "unavailable skills", list(skills.get("unavailableSkillIds")));
            CliText.appendBulletBlockIfAny(output, "surface-incompatible skills", list(skills.get("incompatibleSkillIds")));
            CliText.appendBulletBlockIfAny(output, "skill recommendations", list(skills.get("recommendations")));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }
}
