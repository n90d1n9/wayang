package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.Map;

/**
 * Plain-text renderer for aggregate Wayang readiness reports.
 */
final class WayangReadinessTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangReadinessTextFormat() {
    }

    static String text(WayangReadinessReport readiness) {
        StringBuilder output = new StringBuilder("Wayang readiness").append(NL);
        output.append("readinessId: ").append(readiness.readinessId()).append(NL);
        output.append("ready: ").append(CliText.yesNo(readiness.ready())).append(NL);
        output.append("exitCode: ").append(readiness.exitCode()).append(NL);
        output.append("issues: ").append(readiness.issueCount()).append(NL);
        appendReadinessProfile(output, readiness.attributes());
        appendComponentSummaries(output, readiness.attributes().get("componentSummaries"));
        appendIssues(output, readiness.issues());
        return output.toString();
    }

    private static void appendReadinessProfile(StringBuilder output, Map<String, Object> attributes) {
        Object profileId = attributes.get("readinessProfileId");
        if (profileId != null) {
            output.append("profile: ").append(profileId).append(NL);
        }
    }

    private static void appendComponentSummaries(StringBuilder output, Object summaries) {
        if (!(summaries instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        output.append(NL).append("Components:").append(NL);
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> component) {
                output.append("- ")
                        .append(value(component, "readinessId"))
                        .append(": ")
                        .append(CliText.yesNo(component.get("ready")))
                        .append(" (issues: ")
                        .append(value(component, "issueCount"))
                        .append(")")
                        .append(NL);
            }
        }
    }

    private static void appendIssues(StringBuilder output, List<Map<String, Object>> issues) {
        if (issues.isEmpty()) {
            return;
        }
        output.append(NL).append("Issues:").append(NL);
        for (Map<String, Object> issue : issues) {
            output.append("- ")
                    .append(value(issue, "code"))
                    .append(" [")
                    .append(value(issue, "source"))
                    .append("]: ")
                    .append(value(issue, "message"))
                    .append(NL);
        }
    }

    private static String value(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
