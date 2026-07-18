package tech.kayys.wayang.readiness;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangReportMaps;

/**
 * Small factories for shared readiness-envelope map fragments.
 */
public final class WayangReadinessReports {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;
    public static final String AGGREGATE_READINESS_ID = "wayang.readiness.aggregate";

    private static final String DEFAULT_PROBE = "probe";
    private static final String DEFAULT_ISSUE_CODE = "unknown_issue";
    private static final String DEFAULT_ISSUE_SOURCE = "readiness";

    private WayangReadinessReports() {
    }

    public static int exitCode(boolean ready) {
        return ready ? EXIT_SUCCESS : EXIT_FAILURE;
    }

    public static WayangReadinessReport aggregate(WayangReadinessReport... reports) {
        return aggregate(AGGREGATE_READINESS_ID, reports);
    }

    public static WayangReadinessReport aggregate(String readinessId, WayangReadinessReport... reports) {
        return aggregate(readinessId, reports == null ? List.of() : Arrays.asList(reports));
    }

    public static WayangReadinessReport aggregate(List<WayangReadinessReport> reports) {
        return aggregate(AGGREGATE_READINESS_ID, reports);
    }

    public static WayangReadinessReport aggregate(String readinessId, List<WayangReadinessReport> reports) {
        return aggregate(readinessId, reports, Map.of());
    }

    public static WayangReadinessReport aggregate(
            String readinessId,
            List<WayangReadinessReport> reports,
            Map<String, Object> attributes) {
        List<WayangReadinessReport> components = reports(reports);
        boolean ready = components.stream().allMatch(WayangReadinessReport::ready);
        int issueCount = components.stream()
                .mapToInt(WayangReadinessReport::issueCount)
                .sum();
        return WayangReadinessReport.from(
                readinessId,
                ready,
                exitCode(ready),
                issueCount,
                aggregateProbes(components),
                aggregateIssues(components),
                aggregateAttributes(components, attributes));
    }

    public static Map<String, Object> probe(
            String probe,
            boolean required,
            boolean passed,
            int issueCount,
            Map<String, Object> attributes) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("probe", SdkText.trimToDefault(probe, DEFAULT_PROBE));
        values.put("required", required);
        values.put("passed", passed);
        values.put("issueCount", Math.max(0, issueCount));
        values.put("attributes", WayangReportMaps.copyMap(attributes));
        return WayangReportMaps.copyMap(values);
    }

    public static Map<String, Object> issue(
            String code,
            String source,
            String message) {
        return issue(code, source, message, Map.of());
    }

    public static Map<String, Object> issue(
            String code,
            String source,
            String message,
            Map<String, Object> fields) {
        String normalizedCode = SdkText.trimToDefault(code, DEFAULT_ISSUE_CODE);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", normalizedCode);
        values.put("source", SdkText.trimToDefault(source, DEFAULT_ISSUE_SOURCE));
        values.put("message", SdkText.trimToDefault(message, normalizedCode.replace('_', ' ')));
        WayangReportMaps.copyMap(fields).forEach((key, value) -> {
            if (!values.containsKey(key)) {
                values.put(key, value);
            }
        });
        return WayangReportMaps.copyMap(values);
    }

    private static List<WayangReadinessReport> reports(List<WayangReadinessReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        return reports.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<Map<String, Object>> aggregateProbes(List<WayangReadinessReport> reports) {
        return reports.stream()
                .map(report -> probe(
                        report.readinessId(),
                        true,
                        report.ready(),
                        report.issueCount(),
                        report.toMap()))
                .toList();
    }

    private static List<Map<String, Object>> aggregateIssues(List<WayangReadinessReport> reports) {
        return reports.stream()
                .flatMap(report -> report.issues().stream()
                        .map(issue -> aggregateIssue(report, issue)))
                .toList();
    }

    private static Map<String, Object> aggregateIssue(
            WayangReadinessReport report,
            Map<String, Object> issue) {
        Map<String, Object> values = new LinkedHashMap<>(WayangReportMaps.copyMap(issue));
        values.put("componentReadinessId", report.readinessId());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> aggregateAttributes(
            List<WayangReadinessReport> reports,
            Map<String, Object> attributes) {
        List<String> readinessIds = reports.stream()
                .map(WayangReadinessReport::readinessId)
                .toList();
        List<String> failedReadinessIds = reports.stream()
                .filter(report -> !report.ready())
                .map(WayangReadinessReport::readinessId)
                .toList();
        Map<String, Object> values = new LinkedHashMap<>(WayangReportMaps.copyMap(attributes));
        values.put("componentCount", reports.size());
        values.put("readyComponentCount", reports.size() - failedReadinessIds.size());
        values.put("failedComponentCount", failedReadinessIds.size());
        values.put("componentReadinessIds", readinessIds);
        values.put("failedReadinessIds", failedReadinessIds);
        values.put("componentSummaries", reports.stream()
                .map(WayangReadinessReports::componentSummary)
                .toList());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> componentSummary(WayangReadinessReport report) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("readinessId", report.readinessId());
        values.put("ready", report.ready());
        values.put("exitCode", report.exitCode());
        values.put("issueCount", report.issueCount());
        return WayangReportMaps.copyMap(values);
    }
}
